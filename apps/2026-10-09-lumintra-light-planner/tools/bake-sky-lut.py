"""Bake the two twilight LUTs (assets/sky/sky_lut_a.bin, sky_lut_b.bin).

Implements a Bruneton & Neyret (2008) style radiative-transfer model of the atmosphere:

  * spherical geometry (observer on the ground, Earth + atmosphere as concentric spheres);
  * Rayleigh scattering with the 1/lambda^4 spectrum folded into Bruneton's per-channel
    coefficients (the published RGB triplet is the spectrally-integrated CIE-XYZ form);
  * Mie scattering with the Cornette-Shanks/Henyey-Greenstein phase (g = 0.8);
  * an ozone (Chappuis-band) absorption layer with a triangular profile;
  * multiple scattering, approximated by a successive-orders scheme in which the
    first-order ambient (integrated over the whole sky at the observer) drives an isotropic
    source along each ray; the higher orders are folded in with an effective single-scattering
    albedo.  This is an isotropic approximation of the paper's 4-D precompute, not the full
    precompute; the honest limit is recorded in docs/asset-provenance.md.

Output layout is exactly docs/ASSET-FORMATS.md section 2 (little-endian, 64-byte header,
61 float32 E(eps), 61 float32 alpha_s(eps), 61x91x3 float16 cells).

Run:  python tools/bake-sky-lut.py
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import common  # noqa: E402

# --------------------------------------------------------------------------------------
# Atmosphere constants (km, 1/km).  Rayleigh/Mie/ozone coefficients are Bruneton & Neyret's
# published RGB (spectrally integrated) values; Rg/Rt/HR/HM are the paper's.
# --------------------------------------------------------------------------------------
RG = 6360.0                 # ground radius, km
# Bruneton's ceiling is 60 km.  A 60 km ceiling cannot be sunlit once the solar depression
# exceeds ~6.5 degrees: the terminator annulus sits at altitude R (1/cos(eps) - 1), which is
# 35 km at -6 deg, 88 km at -9.5 deg and 315 km at -18 deg.  With the ceiling at 60 km every
# twilight row below ~-16 deg integrated to exactly zero and the -6..-15 deg rows came out
# red, because the only lit air the model could see was marginal high-altitude air lit by a
# path-grazing, Rayleigh-reddened beam.  The ceiling is therefore raised to 120 km, which
# contains the whole illuminated terminator annulus for every eps in the table.  Everything
# else (Rg, HR, HM, ozone profile, beta coefficients, phase functions) is Bruneton's
# published model; the divergence and its honest limit are recorded in docs/asset-provenance.md.
RT = 6480.0                 # top of the modelled atmosphere, km
HR = 8.0                    # Rayleigh scale height, km
HM = 1.2                    # Mie scale height, km
OZONE_CENTER = 25.0         # km
OZONE_HALF_WIDTH = 15.0     # km

BETA_R = np.array([5.802e-3, 13.558e-3, 33.100e-3])   # Rayleigh, 1/km (XYZ channels)
BETA_M = np.array([3.996e-3, 3.996e-3, 3.996e-3])     # Mie, 1/km (grey)
BETA_O = np.array([0.650e-3, 1.881e-3, 0.085e-3])     # ozone absorption, 1/km
MIE_G = 0.8
H_OPTICAL = 20.0            # optically significant atmosphere for the shadow edge, km
SUN_IRRADIANCE = np.array([1.0, 1.0, 1.0])            # unit solar irradiance (model units)

# CIE XYZ (D65) -> linear sRGB
XYZ_TO_SRGB = np.array([
    [3.2404542, -1.5371385, -0.4985314],
    [-0.9692660, 1.8760108, 0.0415560],
    [0.0556434, -0.2040259, 1.0572252],
])

A1_COUNT, A2_COUNT = 61, 91
A1_START, A1_STEP = 10.0, 0.5
A2_START, A2_STEP = 0.0, 1.0

RAY_STEPS = 192             # view-ray samples (quadratic distribution, dense near the observer)
SUN_STEPS = 32              # sun-ray samples per view sample
AMBIENT_ELEV = np.arange(0.0, 90.0 + 1e-9, 5.0)             # 19 elevations
AMBIENT_AZ = np.arange(0.0, 180.0 + 1e-9, 20.0)             # 10 azimuths (symmetric)
FLOAT16_MAX = 65000.0
# Number of terms of the geometric series in the effective single-scattering albedo used to
# fold the higher scattering orders into the isotropic ambient.  A finite order is deliberate:
# see the comment at its use in `bake()` and the honest limit in docs/asset-provenance.md.
MS_ORDERS = 4


def dens_r(h):
    return np.exp(-np.clip(h, -1.0, None) / HR)


def dens_m(h):
    return np.exp(-np.clip(h, -1.0, None) / HM)


def dens_o(h):
    return np.maximum(0.0, 1.0 - np.abs(h - OZONE_CENTER) / OZONE_HALF_WIDTH)


def phase_rayleigh(mu):
    return (3.0 / (16.0 * np.pi)) * (1.0 + mu * mu)


def phase_mie(mu, g=MIE_G):
    return (1.0 - g * g) / (4.0 * np.pi * np.power(1.0 + g * g - 2.0 * g * mu, 1.5))


def _sphere_hit(radius: float, r0: float, mu: float) -> float:
    """Distance to a sphere of `radius` along a ray from radius r0 at cosine `mu` (>=0 if no hit)."""
    disc = r0 * r0 * (mu * mu - 1.0) + radius * radius
    if disc < 0:
        return -1.0
    return -r0 * mu + np.sqrt(disc)


def _ray_samples(t_top, n):
    """Bruneton-style quadratic sample distribution along a ray of length `t_top`.

    `u` is uniform in (0, 1) and the sample distance is `t = t_top * u^2`, so samples crowd
    towards the ray's origin, where the air is densest.  Midpoint rule in `u`; the Jacobian
    `dt/du = 2 * t_top * u` is folded into the returned weight.  Shapes follow `t_top` with a
    trailing axis of length `n`.
    """
    u = (np.arange(n) + 0.5) / n                                   # (n,)
    t = t_top[..., None] * (u * u)
    dt = 2.0 * (t_top[..., None] * u) * (1.0 / n)
    return t, dt


def _ray_setup(elev_deg):
    """Vectorised view-geometry arrays for a 1-D array of elevations (degrees)."""
    e = np.radians(np.asarray(elev_deg, dtype=np.float64))
    sin_e, cos_e = np.sin(e), np.cos(e)
    t_top = -RG * sin_e + np.sqrt(np.maximum(0.0, RT * RT - RG * RG * cos_e * cos_e))
    return sin_e, cos_e, t_top


def _sun_transmittance(px, py, pz, s):
    """Transmittance from atmosphere points P to the top of the atmosphere along the sun
    direction s, zero where the ray hits the Earth (P is in shadow).  All inputs (..., 3)
    with s a shared (3,) direction."""
    # solve H = P + t*s against sphere radius Rg and Rt
    pdots = px * s[0] + py * s[1] + pz * s[2]
    r2 = px * px + py * py + pz * pz
    disc_g = pdots * pdots - (r2 - RG * RG)
    disc_t = pdots * pdots - (r2 - RT * RT)
    t_ground = -pdots - np.sqrt(np.maximum(0.0, disc_g))
    t_top = -pdots + np.sqrt(np.maximum(0.0, disc_t))
    shadowed = (disc_g >= 0.0) & (t_ground > 0.0)
    t_top = np.where(shadowed, 0.0, t_top)

    n = SUN_STEPS
    tt, dtt = _ray_samples(t_top, n)
    hx = px[..., None] + tt * s[0]
    hy = py[..., None] + tt * s[1]
    hz = pz[..., None] + tt * s[2]
    h = np.sqrt(hx * hx + hy * hy + hz * hz) - RG
    dr, dm, do = dens_r(h), dens_m(h), dens_o(h)
    tau = ((BETA_R[None, None, :] * dr[..., None] +
            BETA_M[None, None, :] * dm[..., None] +
            BETA_O[None, None, :] * do[..., None]) * dtt[..., None]).sum(axis=-2)
    # A ray that reaches the ground before the top of the atmosphere is in the Earth's
    # shadow: no direct sunlight arrives, so the transmittance is zero (not one).
    return np.where(shadowed[..., None], 0.0, np.exp(-tau))


def integrate_sky(eps_deg, az_deg, elev_deg, ambient=None):
    """Compute the single-scattering (and, if `ambient` is given, the isotropic multiple-
    scattering) radiance for a set of view elevations in the vertical plane at azimuth
    `az_deg` relative to the Sun's azimuth.  Returns XYZ radiance, shape (N, 3)."""
    e_sun = np.radians(eps_deg)
    s = np.array([np.cos(e_sun), 0.0, np.sin(e_sun)])          # sun at azimuth 0

    sin_h, cos_h, t_top = _ray_setup(elev_deg)
    a = np.radians(az_deg)
    v = np.stack([cos_h * np.cos(a), cos_h * np.sin(a), sin_h], axis=-1)   # (N,3)
    mu_los = v @ s                                                        # cos(angle to sun)

    n = RAY_STEPS
    tt, dtt = _ray_samples(t_top, n)                                     # (N,n)
    px = v[:, 0][:, None] * tt
    py = v[:, 1][:, None] * tt
    pz = RG + v[:, 2][:, None] * tt
    # sample radii may dip below RG only if h<0; h>=0 here
    h = np.sqrt(px * px + py * py + pz * pz) - RG
    dr, dm, do = dens_r(h), dens_m(h), dens_o(h)

    # view transmittance from observer to P
    tau_view = ((BETA_R[None, None, :] * dr[:, :, None] +
                 BETA_M[None, None, :] * dm[:, :, None] +
                 BETA_O[None, None, :] * do[:, :, None]) * dtt[:, :, None]).cumsum(axis=1)
    t_view = np.exp(-tau_view)

    t_sun = _sun_transmittance(px, py, pz, s)                            # (N,n,3)

    sigma_r = BETA_R[None, None, :] * dr[:, :, None]
    sigma_m = BETA_M[None, None, :] * dm[:, :, None]
    p_r = phase_rayleigh(mu_los)[:, None, None]
    p_m = phase_mie(mu_los)[:, None, None]
    src = (sigma_r * p_r + sigma_m * p_m) * t_sun * SUN_IRRADIANCE[None, None, :]
    l1 = (t_view * src * dtt[:, :, None]).sum(axis=1)                    # (N,3)

    if ambient is None:
        return l1

    sigma_s = sigma_r + sigma_m
    j_ms = sigma_s * (ambient[None, None, :] / (4.0 * np.pi))            # (N,n,3)
    l_ms = (t_view * j_ms * dtt[:, :, None]).sum(axis=1)
    return l1 + l_ms


def zenith_ambient(eps_deg):
    """First-order ambient irradiance (XYZ) at the observer, integrated over the sky."""
    total = np.zeros(3)
    for az in AMBIENT_AZ:
        rows = []
        for h in AMBIENT_ELEV:
            rows.append(integrate_sky(eps_deg, az, [h], None)[0])
        l = np.array(rows)                        # (Nеlev, 3)
        d_h = np.radians(5.0)
        d_az = np.radians(20.0)
        w = np.cos(np.radians(AMBIENT_ELEV)) * d_h * d_az
        mult = 1.0 if az in (0.0, 180.0) else 2.0
        total += mult * (l * w[:, None]).sum(axis=0)
    return total


def effective_albedo(eps_deg):
    """Column effective single-scattering albedo per channel for the ambient multiple-scattering
    fold: tau_scatter / (tau_scatter + tau_ozone), evaluated on a representative slant path."""
    mu0 = max(np.sin(np.radians(abs(eps_deg))), 0.05)
    # vertical optical depths
    n = 200
    h = np.linspace(0.0, RT - RG, n)
    dz = h[1] - h[0]
    tau_r = (BETA_R[:, None] * dens_r(h)[None, :] * dz).sum(axis=1)
    tau_m = (BETA_M[:, None] * dens_m(h)[None, :] * dz).sum(axis=1)
    tau_o = (BETA_O[:, None] * dens_o(h)[None, :] * dz).sum(axis=1)
    slant = 1.0 / mu0
    sca = (tau_r + tau_m) * slant
    abs_ = tau_o * slant
    return sca / (sca + abs_ + 1e-12)


def alpha_shadow(eps_deg):
    """Earth-shadow edge altitude (degrees) -- design/O SS5.4, reproduced exactly by
    alpha_s = |eps| + asin((R/2H) * eps_rad^2), capped at 90, floored at eps."""
    e = np.radians(eps_deg)
    k = RG / (2.0 * H_OPTICAL) * (e * e)
    out = np.where(k >= 1.0, 90.0, np.degrees(np.abs(e) + np.arcsin(np.clip(k, 0.0, 1.0))))
    return np.maximum(out, eps_deg)


def gamut_map_linear_srgb(rgb):
    """Map out-of-gamut linear sRGB tristimulus into the non-negative octant.

    The XYZ -> sRGB matrix has negative coefficients, so a physically meaningful spectrum can
    land slightly outside the sRGB solid (a saturated twilight arc does).  Clipping such a
    value to zero punches a hole in a channel; instead the value is blended towards the
    equal-energy point of the *same* luminance -- the standard desaturating gamut map -- which
    preserves Y exactly and keeps the lattice continuous.  Applied to every baked cell.
    """
    y = rgb @ np.array([0.2126729, 0.7151522, 0.0721750])
    y3 = y[..., None]
    lo = rgb.min(axis=-1, keepdims=True)
    t = np.where(lo < 0.0,
                 np.clip(-lo / np.maximum(y3 - lo, 1e-30), 0.0, 1.0),
                 0.0)
    return (1.0 - t) * rgb + t * y3


def bake():
    a1 = A1_START - A1_STEP * np.arange(A1_COUNT)          # +10 .. -20
    a2 = A2_START + A2_STEP * np.arange(A2_COUNT)          # 0 .. 90
    cells_a = np.zeros((A1_COUNT, A2_COUNT, 3), dtype=np.float64)
    cells_b = np.zeros((A1_COUNT, A2_COUNT, 3), dtype=np.float64)
    e_eps = np.zeros(A1_COUNT, dtype=np.float64)

    for i, eps in enumerate(a1):
        amb = zenith_ambient(eps)
        alb = effective_albedo(eps)
        # Fold the higher scattering orders as a FINITE geometric series in the effective
        # single-scattering albedo.  The infinite sum amb/(1 - w0) is deliberately not taken:
        # with an isotropic ambient and w0 -> 1 in the blue (ozone is nearly transparent
        # there) it diverges far past what this approximation can justify.  Four terms keep
        # the physically correct ordering (blue scatters more than red) with a bounded
        # amplification of at most 4x, and the choice is recorded as the model's honest
        # limit in docs/asset-provenance.md.
        amb_tot = amb * sum(alb ** k for k in range(MS_ORDERS))
        l_a = integrate_sky(eps, 180.0, a2, amb_tot)
        l_b = integrate_sky(eps, 0.0, a2, amb_tot)
        # CIE XYZ -> linear sRGB, then into the non-negative octant
        srgb_a = gamut_map_linear_srgb(l_a @ XYZ_TO_SRGB.T)
        srgb_b = gamut_map_linear_srgb(l_b @ XYZ_TO_SRGB.T)
        cells_a[i] = np.clip(srgb_a, 0.0, None)
        cells_b[i] = np.clip(srgb_b, 0.0, None)
        # exposure = CIE luminance (Y) of the zenith view (h = 90 is the last row)
        e_eps[i] = max(l_a[-1, 1], 1e-14)
        print(f"  eps={eps:+6.2f}  E={e_eps[i]:.6e}  maxA_rel="
              f"{(cells_a[i].max(axis=0) / e_eps[i]).max():.4g}  alpha_s={alpha_shadow(eps):.2f}",
              flush=True)

    rel_a = cells_a / e_eps[:, None, None]
    rel_b = cells_b / e_eps[:, None, None]
    clipped = int(((rel_a > FLOAT16_MAX).sum() + (rel_b > FLOAT16_MAX).sum()))
    rel_a = np.clip(rel_a, 0.0, FLOAT16_MAX)
    rel_b = np.clip(rel_b, 0.0, FLOAT16_MAX)
    return a1, a2, e_eps, alpha_shadow(a1), rel_a, rel_b, clipped


def write_lut(path: Path, e_eps, alpha_s, cells):
    import struct

    buf = bytearray()
    buf += b"LUMSKY01"
    buf += struct.pack("<IHH", 1, A1_COUNT, A2_COUNT)
    buf += struct.pack("<ffff", A1_START, A1_STEP, A2_START, A2_STEP)
    buf += struct.pack("<B", 3)
    buf += b"\x00" * 31
    assert len(buf) == 64, len(buf)
    buf += struct.pack("<61f", *e_eps)
    buf += struct.pack("<61f", *alpha_s)
    arr = np.asarray(cells, dtype=np.float16)
    # enforce i1-major, i2, channel order: cells already (61,91,3) C-contiguous
    buf += arr.astype("<f2").tobytes(order="C")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(bytes(buf))
    return len(buf)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out-dir", type=Path, default=common.ASSETS / "sky")
    args = ap.parse_args()

    print("baking sky LUTs (Bruneton-Neyret, single + isotropic multiple scattering)...", flush=True)
    a1, a2, e_eps, alpha_s, rel_a, rel_b, clipped = bake()
    n_a = write_lut(args.out_dir / "sky_lut_a.bin", e_eps, alpha_s, rel_a)
    n_b = write_lut(args.out_dir / "sky_lut_b.bin", e_eps, alpha_s, rel_b)
    print(f"wrote sky_lut_a.bin {n_a} B, sky_lut_b.bin {n_b} B, clipped cells={clipped}")
    assert n_a == 33858 and n_b == 33858, (n_a, n_b)


if __name__ == "__main__":
    main()
