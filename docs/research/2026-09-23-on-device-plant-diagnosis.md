# On-device plant-disease diagnosis — what is actually achievable

**Read on:** 2026-09-23 (session date; operator timezone UTC+3). Every dataset size,
model, licence, and API fact below was read from the URL cited next to it on that
date. Nothing here is recalled from training data.

**This is the ON-DEVICE half of the fork.** The HOSTED half is
[`./2026-09-23-deepseek-api.md`](./2026-09-23-deepseek-api.md) and is not redone here —
that document establishes the models, the prices, the vision constraint, and the
proxy/TOS requirement. Its numbers are referenced, not re-derived.

**Governing rule:** if a fact could not be read from a source it is marked
**UNVERIFIED** in §7, with a statement of what could not be confirmed. No gap is
filled with a plausible number. This follows
[`../../.cursor/rules/verification-discipline.mdc`](../../.cursor/rules/verification-discipline.mdc).

---

## 0. Verdict

1. **On-device diagnosis is buildable by this factory.** It is Kotlin + Compose +
   a LiteRT model asset. No server, no API key, no per-diagnosis cost, works offline.
   That fork is technically open. (§3)

2. **But the planned crop set is not covered.** The plan is tomatoes, cucumbers and
   peppers. In **both** confirmed datasets, **cucumber does not exist** (§1.3).
   Pepper exists in only **two classes** (bacterial spot + healthy). Tomato is the
   only one of the three with real depth (~9–10 disease classes).
   **A cucumber diagnosis cannot be trained from any dataset confirmed in this
   session.**

3. **The licence is the quiet blocker, and it is not about the images — it is about
   the trained model.** PlantVillage is CC BY-SA 3.0, and its authors state that
   "algorithms trained on the data fall under the same license" (§1.1). That is a
   **share-alike obligation on the shipped model**, and it is the opposite of what a
   proprietary APK wants. The GitHub repo carries **no LICENSE file at all**, so the
   licence rests on the authors' paper text. PlantDoc, by contrast, is **CC BY 4.0**
   and is commercially usable (§1.2) — but it is only 2,598 images.

4. **Google publishes exactly one relevant artefact, and it is not for these crops.**
   **CropNet** is Google-published, **Apache-2.0**, MobileNetV3-based, and its
   *classifier* diagnoses **cassava only** — the model card says plainly it "does not
   detect diseases in other plants" (§2.1). Its *feature vectors* (Apache-2.0) are a
   legitimate, distributable backbone to fine-tune **our own** head on. There is no
   Google-published tomato/pepper/cucumber model. ML Kit ships none either (§2.2).

5. **Deploying a model means shipping native code, which is exactly the case the
   16 KB rule is about.** The official Android page says an app uses native code if it
   "links with any third-party native libraries or dependencies (such as SDKs)". LiteRT
   ships `.so` files. Google Play stops accepting updates without 16 KB support from
   **2027-02-01** (§3.1). LiteRT claims 16 KB support from 1.4.0, but environment
   reports disagree about specific artefacts, so **this must be verified per build with
   the documented check, never assumed** (§3.2).

6. **The accuracy claim is the thing that should stop the project as currently
   framed.** The PlantVillage authors' own paper reports **99.35%** on the held-out
   test set and **31.40% / 31.69%** on real-world images — and even when the crop is
   given to the model, accuracy is **47.8%** (§6). A separate 2026 study measured
   **99.73% → 32.05%** with **79.76% mean confidence** still intact. A feature that
   tells a grower what to spray on food cannot ship on that evidence.

7. **Recommendation: on-device, but scoped down and with the advisory claim removed.**
   Confidence **MEDIUM** on the architecture, **LOW** on diagnostic quality. The one
   missing measurement that would settle the fork is named in §5.3.

---

## 1. Datasets

### 1.1 PlantVillage — confirmed, but the licence reaches the trained model

**What it is.** The best-known open crop-disease image set.

**Size — three sources disagree, and all three are cited here rather than averaged:**

| Source | Figure | URL (read 2026-09-23) |
|---|---|---|
| Original paper (Hughes & Salathé, 2015) | "The data records contain **54,309 images**" | <https://arxiv.org/pdf/1511.08060> |
| Dataset repo README | "**54,306 images** … **14 crop species** and **26 diseases**" | <https://github.com/spMohanty/PlantVillage-Dataset> |
| TensorFlow Datasets catalogue | "**54303** healthy and unhealthy leaf images … **38 categories**" | <https://www.tensorflow.org/datasets/catalog/plant_village> |
| HuggingFace mirror `mohanty/PlantVillage` | "**54,306 images** … 14 crop species and 26 diseases"; splits train **43,596** / test **10,709** | <https://huggingface.co/datasets/mohanty/PlantVillage> |

The 54,303 / 54,306 / 54,309 spread is a real discrepancy between distributions, not a
typo here. Use **"~54.3k images, 38 classes, 14 crop species"** and cite a specific
source for whatever number is quoted downstream.

**Crop species** (paper, read above — note it is a complete list, and note what is
*not* in it):

> "The images span 14 crop species: Apple, Blueberry, Cherry, Corn, Grape, Orange,
> Peach, **Bell Pepper**, Potato, Raspberry, Soybean, Squash, Strawberry, **Tomato**."

**Licence — the important part.** From the dataset's own paper (read 2026-09-23):

> "Over 50,000 images of these are now stored on www.plantvillage.org, openly accessible
> and released under the Creative Commons Attribution-ShareAlike 3.0 Unported
> (CC BY-SA 3.0), **with the clarification that algorithms trained on the data fall
> under the same license.**"

The same sentence, with the same clarification, appears on the PlantVillage challenge
page: "All images are released under the Creative Commons Attribution-ShareAlike 3.0
Unported (CC BY-SA 3.0), with the clarification that algorithms trained on the data
fall under the same license."
(<https://www.aicrowd.com/challenges/plantvillage-disease-classification-challenge>, read 2026-09-23>)

The HuggingFace mirror's dataset card declares `license: cc-by-sa-3.0` — confirmed via
the HuggingFace API, not by eye.

**Two licence defects that matter more than the licence itself:**

- **The GitHub repository has no licence file.** The GitHub API reports
  `"license": null` for `spMohanty/PlantVillage-Dataset`, and its root contains
  `README.md`, `CITATION.cff`, `raw/`, `leaf_grouping/` and scripts — but no `LICENSE`
  file. So the CC BY-SA 3.0 claim, **including the share-alike-on-trained-models
  clause**, exists only in the authors' paper prose and on a third-party challenge page.
  A licence whose most consequential term is stated in a paper rather than a licence
  file is a term a lawyer should read before a model ships.
- **A re-upload has re-declared it as CC0.** `geraldmc/plantvillage-full` declares
  `license: cc0-1.0` (confirmed via the HuggingFace API). At least one model in §2.3
  trains on it and reports that as its lineage. A third party cannot relicense CC BY-SA
  material as CC0, so that declaration **does not** clean the licence. Treat any
  "CC0 PlantVillage" claim as **UNVERIFIED and unreliable**.

**A second, subtler defect: leaf leakage.** The HuggingFace card warns that "Multiple
images often capture the same physical leaf; separating them across train/test sets
will bias your evaluation", and exposes a `leaf_id` field for grouped splitting. Any
accuracy number produced on a split that ignores `leaf_id` is inflated. This is a
concrete way to reproduce the optimistic numbers in §6.

**A size discrepancy worth flagging:** TFDS names
<https://data.mendeley.com/datasets/tywbtsjrjv/1> as the dataset URL, but that page
describes something different — "**39** different classes … **61,486 images**", built
with "six different augmentation techniques", and it adds a `Background_without_leaves`
class that PlantVillage does not have. It is an augmented derivative, not the canonical
set. **Its licence is UNVERIFIED** — the page as read did not state one.

### 1.2 PlantDoc — the licence-clean alternative

- **License: CC BY 4.0** — the LICENCE file is
  `Creative Commons Attribution 4.0 International`
  (<https://github.com/pratikkayal/PlantDoc-Dataset/blob/master/LICENSE.txt>, read 2026-09-23>).
  A Kaggle mirror states the same and adds "You may use it commercially without
  Liability, Trademark use, Patent use, or Warranty"
  (<https://www.kaggle.com/datasets/andresmgs/plantdec>, read 2026-09-23>).
- **Size:** "2,598 data points in total across **13 plant species** and up to **17**
  classes of diseases" (paper abstract, <https://arxiv.org/abs/1911.10317>, read 2026-09-23>).
- **It is field-condition data by design:** "we focus on images in natural environmental
  conditions with non-trivial background noise". That is exactly the distribution a
  phone photo lands in, and exactly what PlantVillage lacks.
- **What it is for:** it is a *benchmark*, not a training corpus. 2,598 images is small.

**This is the licence decision in one line: PlantVillage (big, 38 classes, share-alike
reaching the trained model) versus PlantDoc (permissive, commercial, but 2.6k images
and field-noisy).**

### 1.3 Crop coverage against the planned crops — the cucumber gap

Confirmed from the datasets' own class lists (PlantVillage class list via
<https://huggingface.co/datasets/mohanty/PlantVillage> and the Mendeley derivative page;
PlantDoc class list read directly from the repository's `train/` directory):

| Planned crop | PlantVillage | PlantDoc | Verdict |
|---|---|---|---|
| **Tomato** | ✅ 10 classes (bacterial spot, early blight, late blight, leaf mold, Septoria leaf spot, spider mites, target spot, yellow leaf curl virus, mosaic virus, healthy) | ✅ 9 classes | **Serviceable** |
| **Pepper** | ⚠️ **2 classes only** (`Pepper_bacterial_spot`, `Pepper_healthy`) | ⚠️ **2 classes only** (`Bell_pepper leaf spot`, `Bell_pepper leaf`) | **Thin — one disease, or nothing** |
| **Cucumber** | ❌ **absent** | ❌ **absent** | **No training data** |

Cucumber appears in **neither** dataset's class list. It is not a gap that more data
collection solves inside this project — it means the stated intent ("tomatoes,
cucumbers, peppers") cannot be met by training a classifier on these datasets, and a
cucumber diagnosis would be a fabricated capability. This is the single most important
planning finding in this document.

### 1.4 Licence options, stated plainly

| Option | What it means | Status |
|---|---|---|
| Train on PlantVillage | Must ship the resulting model under **CC BY-SA 3.0** per the authors' clarification, unless counsel reads that clause differently | Licence term read from the paper; **its legal effect on a shipped app is UNVERIFIED** |
| Train on PlantDoc | CC BY 4.0 — attribution only, commercial use permitted | **Confirmed** — but only 2,598 images, fewer crops |
| Train on the CC0 re-upload | Would be clean *if* the relicensing were valid | **UNVERIFIED and unreliable** — a third party cannot relicense CC BY-SA as CC0 |
| Fine-tune Google's CropNet head | Backbone is Apache-2.0 | Backbone **confirmed**; does **not** clean the training data of the head |

**This is a decision for the human plus counsel, not for an agent.** The factory's own
rule is that legal declarations need human approval
([`../../AGENTS.md`](../../AGENTS.md), "Working rules").

---

## 2. Ready-made models and Android-ready artefacts

### 2.1 Google CropNet — Apache-2.0, but cassava-only as a classifier

**This is the only Google-published model relevant to crop disease that this session
found.** Read 2026-09-23 from
<https://tfhub.dev/google/cropnet/classifier/cassava_disease_V1/2> (which now resolves to
Kaggle Models, `google/cropnet`):

| Property | Value |
|---|---|
| Publisher | **Google** (profile "Google", published 2020-10-06) |
| **Licence** | **"This model follows the Apache 2.0 license."** The variation entry repeats "License Apache 2.0" |
| Architecture | MobileNet V3 |
| Input | float tensor `[batch size, 224, 224, 3]`, RGB scaled to `[0, 1]` |
| Output (feature vector) | `image_feature_vector`, float `[batch size, 1280]` |
| Training data (feature vector) | concatenation of **iNaturalist** and the **plants subset of ImageNet-21K** — "2.15M images and 8864 classes" |
| Fine-tunable | Yes (`trainable=True`) |

**The classifier limitation, in the source's own words:**

> "This model is trained to recognize four Cassava diseases."
> "**This model does not detect diseases in other plants.**"
> "This model assumes that the input contains a well-cropped image of a Cassava plant.
> If the image contains some other plant, the output results may be meaningless."
> "…was trained only on photos of diseased cassava plants taken in Uganda."

So: **CropNet is not a tomato or pepper model.** What it *is* is an
**Apache-2.0, Google-published, mobile-sized feature extractor** — and Google ships an
official tutorial for exactly the workflow this factory would need: fine-tune a CropNet
feature vector, then "Export a TFLite model, ready to be deployed on your app with Task
Library, MLKit or TFLite directly"
(<https://www.tensorflow.org/hub/tutorials/cropnet_on_device>, read 2026-09-23>). The
tutorial's own guidance is that "the same technique used here could be applied to other
plant diseases tasks".

**Artefact size:** the model page's file explorer showed `saved_model.pb` at **1.52 MB**
— that is a SavedModel tensor, not a TFLite flatbuffer. **The exact TFLite artefact size
is UNVERIFIED** (the page did not state it unambiguously).

### 2.2 Google ML Kit ships no plant model

| ML Kit API | What it actually offers | Source (read 2026-09-23) |
|---|---|---|
| Image labeling (base) | "Recognizes more than **400** categories that describe the most commonly found objects in photos" — general-purpose | <https://developers.google.com/ml-kit/vision/image-labeling> |
| Object detection, coarse classifier | **Five** categories: "home goods, fashion goods, food, **plants**, and places" | <https://developers.google.com/ml-kit/vision/object-detection> |
| Custom models | Both APIs "allow you to replace the default models" with a custom image-classification model from TensorFlow Hub, AutoML, or your own | <https://developers.google.com/ml-kit/custom-models> |

**Conclusion:** ML Kit can tell you an image contains a *plant*. It cannot tell you a
plant is diseased. Using ML Kit therefore means supplying **your own** TFLite model —
which puts you in §2.3 and §3, not in a turn-key API. Note also that "plants" as a
coarse category does nothing to reduce this section's other problems.

### 2.3 Third-party TFLite models found, with their licences

These are real artefacts with real licences. **None is a Google release, none is
audited, and every accuracy figure is the source's own — reported-by-source, never
verified by us.**

| Model | Licence | Input / format | Reported accuracy | File size | Usable for this product? |
|---|---|---|---|---|---|
| **AgriVision AI** (`peachbotAI/agrivision-mobilenetv2-edge-tpu`) | **Apache-2.0** (model card + HF API) | MobileNetV2 α=0.35, 224×224 RGB, full-integer **UINT8** TFLite | **98.66 %** FLOAT / **98.58 %** INT8, held-out PlantVillage, **self-reported** | `plant_health_int8.tflite` = **627,400 B**; `plant_health_edgetpu.tflite` = **778,880 B** (HF API, exact) | **No.** Binary `healthy`/`problem` **only** |
| **GreenHealer** (`MustafaBeratYavas/plant-disease-edge-ai-diagnosis-system`) | **MIT** (the *code*; the trained model derives from PlantVillage) | MobileNetV3Large, 224×224, 38-class softmax, PTQ int8 with float32 I/O | **99.21 %** on its own PlantVillage split (5,416/5,459), self-reported | **3.0 MB**; 3,032,870 params; 5.58 ms latency (CPU) | Partly — 38-class, but Flutter packaging and a licence question |
| **CropNet classifier** (`google/cropnet`) | **Apache-2.0** | MobileNet V3, 224×224×3 [0,1] | not stated as a single figure | not stated | **No** — cassava only |

Read from <https://huggingface.co/peachbotAI/agrivision-mobilenetv2-edge-tpu> and
<https://github.com/MustafaBeratYavas/plant-disease-edge-ai-diagnosis-system> (both read
2026-09-23); file sizes confirmed from the HuggingFace API, not from prose.

**What AgriVision's own card says about itself, verbatim** — this is the most useful
paragraph in §2 and it should be taken seriously because it is the *model author's* own
disclaimer:

> "the reported accuracy is held-out evaluation on PlantVillage imagery. **It is not a
> field-accuracy claim** and does not establish performance across real farms, lighting
> conditions, cultivars, camera positions, or all plant diseases/stressors."
>
> "**Not intended for:** professional agricultural diagnosis; **pesticide or treatment
> recommendations**; claims of general field accuracy; safety-critical crop-management
> decisions…"
>
> "Hardware validation | **Pending** physical Raspberry Pi + Coral run"

Three consequences:

1. **A binary `healthy`/`problem` model cannot support the product.** It cannot name a
   disease, therefore it cannot tell anyone what to spray. If the differentiator is a
   *spray/act* recommendation, this artefact does not reach it.
2. **It is published for Raspberry Pi + Coral Edge TPU, not Android.** The `.tflite`
   files are valid TFLite and could be loaded by LiteRT on Android, but **Android
   deployment was not the publisher's target and is unproven** — treat that step as
   **INFERRED, not verified**.
3. **Its own lineage says `Dataset license CC0-1.0`** (`geraldmc/plantvillage-full`).
   That is the re-upload flagged in §1.1, and it does not resolve the share-alike
   question.

### 2.4 Published papers with no released Android artefact

For completeness, and to show the reported-accuracy landscape. **These are papers, not
downloadable models, and no licence for a deployable artefact was confirmed.**

| Work | Reported accuracy | Notes | URL (read 2026-09-23) |
|---|---|---|---|
| **ParaLeafNet** | 98.7 % base → ~99.5 % optimized; severity 92.3 % | 38-class + severity, 5.8 M params, **14.2 MB TFLite INT8**, 78 ms on Snapdragon 865 | <https://impactfactor.org/PDF/IJDDT/16/IJDDT,Vol16,Issue15s,Article8.pdf> |
| **RTR_Lite_MobileNetV2** | **99.92 %** on "Plant Disease"; **82.00 %** on PlantDoc | Same paper: 97.11 % PaddyDoctor, 90.84 % Coffee, 100 % Wheat | <https://www.sciencedirect.com/science/article/pii/S2214662825000271> |

Note the shape of that RTR row: **99.92 % on the lab set, 82.00 % on the field set, in
the same paper.** That is consistent with §6 and is the honest way to read every
"99 %+" claim in this document.

---

## 3. Can it run inside the factory's constraints?

### 3.1 It requires native code — and that is precisely the 16 KB rule

**This section is based on a document that was read** (the official Android page-size
guide, **last updated 2026-09-16**,
<https://developer.android.com/guide/practices/page-sizes>), plus the LiteRT
documentation and the LiteRT issue tracker.

The official guide defines native code broadly:

> "Your app makes use of native code if any of the following apply:
> - Your app uses any C/C++ (native) code…
> - **Your app links with any third-party native libraries or dependencies (such as
>   SDKs) that use them.**"

And it sets the release consequence:

> "all apps targeting Android 15 (API level 35) and higher must support 16 KB memory
> page sizes on 64-bit devices on Google Play. **Starting February 1, 2027, if your app
> updates don't support 16 KB memory page sizes, you won't be able to release these
> updates.**"

LiteRT on Android is a native runtime. The documentation lists the Maven dependency
`com.google.ai.edge.litert:litert` (the Google Maven metadata read on 2026-09-23 lists
`<latest>2.2.0</latest>`, last updated `20260813`), and Google's own engineers describe
inspecting its ELF headers:

> "I downloaded the `litert:1.4.0` AAR directly from Maven Central, extracted the native
> library (**`libtensorflowlite_jni.so`**), and inspected its ELF header using readelf."
> — <https://github.com/google-ai-edge/LiteRT/issues/4286>

A `.so` in the AAR is a native dependency in the app. **Therefore: bundling a LiteRT
model puts the app in scope for the 16 KB requirement.**

**The factory is already on the right side of the packaging half of this.** The official
guide's shortcut is: "If you update your tools to the latest versions (**AGP version
8.5.1 or higher** and **NDK version r28 or higher**) and use 16 KB-compatible prebuilt
dependencies, then your app is 16 KB compatible by default." `AGENTS.md` fixes **AGP
9.4.1**, well above 8.5.1. The remaining risk is entirely in **third-party prebuilt
`.so` files** — i.e. LiteRT itself and anything it pulls in transitively.

Note also that the guide's escape hatch for old AGP — `jniLibs.useLegacyPackaging = true`
— is on `AGENTS.md`'s "Defaults to suppress" list. **The documented workaround is
forbidden by the factory's own contract.** The only compliant path is 16 KB-ready
dependencies.

**One favourable fact:** LiteRT's documented minimum SDK is **23** (Android 6
Marshmallow) per <https://developers.google.com/edge/litert/android>, and the factory
requires `minSdk 26` (`AGENTS.md`). **No conflict** — LiteRT's floor is below the
factory's.

### 3.2 LiteRT's 16 KB status is artefact-specific — verify it, do not assume it

This is where the sources disagree, and the disagreement is the finding.

| Claim | Source | What it actually says |
|---|---|---|
| "LiteRT **1.4.0+** provides 16 KB compatibility" | <https://discuss.ai.google.dev/t/tensorflow-lite-and-16-kb-page-size-support/93052> | A Google AI developer forum answer recommends migrating to LiteRT 1.4.0 |
| "The `litert:1.4.0` AAR from Maven Central is correctly built with **16KB page alignment**… `p_align` value of `0x4000`" | <https://github.com/google-ai-edge/LiteRT/issues/4286> | Google engineers verified it; the issue was closed |
| "`liblitert_jni.so` in **LiteRT 2.0.1-alpha** is indeed compiled with 16KB page alignment" | <https://github.com/google-ai-edge/LiteRT/issues/8> | A Google engineer's `greadelf` output |
| "only **`libimage_processing_util_jni.so`** (shipped inside `com.google.ai.edge.litert:litert-support:1.4.0`) is built for 4 KB pages, while the others use 16 KB" — producing a Play upload error | <https://stackoverflow.com/questions/79786888/…> | A user's real Play upload failure, with `readelf` output |
| "As of version 2.17.0, the official `tensorflow-lite` AAR … is **not** built with 16KB page alignment" | <https://github.com/tensorflow/tensorflow/issues/96602> | Google confirms the **legacy `org.tensorflow:tensorflow-lite`** artefact is not compliant; recommends LiteRT |

**Synthesis, stated as what it is:**

- The **legacy TensorFlow Lite artefact** (`org.tensorflow:tensorflow-lite`) is
  confirmed non-compliant. **Do not use it.** Note that `AGENTS.md`'s toolchain table
  does not currently name a LiteRT or TFLite dependency, so there is nothing to correct
  there yet — but a future slot must pick **LiteRT, not legacy TFLite**.
- **LiteRT claims compliance from 1.4.0**, and Google engineers verified the main
  `.so`. But a **companion artefact shipped a 4 KB-aligned library**, and it broke a real
  Play upload. So "LiteRT is 16 KB-ready" is not a safe blanket statement.
- **Therefore the rule for a slot is: verify, per build, with the documented check.**
  The official page gives it precisely: extract the APK, run
  `llvm-objdump -p <file>.so | grep LOAD` on each shared object under `lib/`, and
  confirm **no LOAD segment has an alignment below `2**14`**; then run
  `zipalign -v -c -P 16 4 APK_NAME.apk` and require "Verification successful".

**Marked UNVERIFIED:** that **LiteRT 2.2.0** specifically ships 16 KB-aligned `.so`
across **all** of its artefacts. This session read Google's claims about 1.4.0/2.0.1 but
did not inspect a 2.2.0 AAR, and **Gradle must not be run for this task**, so no build
evidence exists. **This is a five-minute check on the first slot that bundles LiteRT,
and it should be done before any model work.**

### 3.3 Is there a pure-Java/Kotlin path that avoids native code?

**Short answer: not on the documented path, and nothing credible for a CNN was
verified.**

- The **documented** Android path is LiteRT (`CompiledModel` API for new work; the
  legacy `Interpreter` API is "strictly deprecated for new features" per
  <https://github.com/google-ai-edge/litert>) or ML Kit with a **custom** model — both
  native.
- The official Android guide is explicit that a Java/Kotlin-only app needs no work:
  *"If your app only uses code written in the Java programming language or in Kotlin,
  including all libraries or SDKs, then your app already supports 16 KB devices."*
  Avoiding native code is therefore a **real** way out of §3.1–3.2 — but only if the
  inference itself is pure JVM.
- Pure-JVM deep-learning libraries **do exist** — e.g. **Deep Netts** (a pure-Java deep
  learning library, <https://www.deepnetts.com/>) and **SKaiNET** (Kotlin Multiplatform,
  JVM target, <https://github.com/sk-ai-net/SKaiNET>). A pure-Java CNN from scratch is
  also demonstrably possible.

**Marked UNVERIFIED, and deliberately so:** whether any pure-JVM engine can run a
MobileNet-class CNN on an Android phone with acceptable latency and memory, and whether
it reproduces a LiteRT model's outputs. **Nothing in this session tested that.** The
pure-JVM route is a genuine option in principle and an unproven one in practice; it
would need its own spike before it could be chosen.

**This is based partly on documents read (LiteRT docs, Android page-size guide) and
partly on inference** (that an AAR containing `libtensorflowlite_jni.so` means the app
"links with a third-party native library"). The inference is straightforward but it is
labelled.

### 3.4 App-size cost — measured, not estimated

A model file is an APK asset. Exact bytes, from the HuggingFace API and Google Maven
(both fetched 2026-09-23):

| Component | Exact size | Note |
|---|---|---|
| AgriVision `plant_health_int8.tflite` | **627,400 B** (~613 KB) | binary, the smallest option |
| AgriVision `plant_health_edgetpu.tflite` | 778,880 B | Edge TPU compiled variant |
| GreenHealer quantized TFLite | **~3.0 MB** | 38-class, source's own figure |
| ParaLeafNet INT8 TFLite | **14.2 MB** | paper's figure |
| CropNet `saved_model.pb` | 1.52 MB | SavedModel tensor, **not** the TFLite artefact |
| **LiteRT runtime** `litert:2.2.0` AAR | **11,761,541 B** (~11.76 MB) | Google Maven |
| `litert:2.1.5` AAR | 10,058,192 B | Google Maven |
| `litert-api:2.2.0` AAR | 934,666 B | Google Maven |

**Read this correctly:** the AAR size is the **whole multi-ABI artifact**, not the
installed delta. App Bundle ABI splits plus R8 shrink it substantially, and the guide
notes a small size *increase* from 16 KB alignment. **The real installed-size delta is
UNVERIFIED** because measuring it needs a build, and Gradle was excluded from this task.

The honest qualitative statement: **for this product the runtime is likely the larger
half of the cost.** Bundling a 613 KB model does not mean the app grows by 613 KB; it
means the app grows by a single-ABI slice of an ~11.8 MB AAR **plus** its model. If a
slot's size budget is tight, that is where the bytes are — and `litert-api`,
`litert-gpu:2.1.5` and `litert-api:2.1.5` returned 404 from the Google Maven path used
here, so their sizes are **UNVERIFIED** and must not be guessed.

---

## 4. On-device vs hosted

**Hosted figures are taken from [`./2026-09-23-deepseek-api.md`](./2026-09-23-deepseek-api.md)
and are not re-derived.** From that document: prices in §2.1 (USD per 1M tokens), the
peak/off-peak rule in §2.2, the image token ceiling in §4.4, the model/vision table in
§1, the required proxy in §5.1–5.3, and the `K` multiplier table in §6.4.

**A vision-capable model is mandatory for the hosted path, and exactly one DeepSeek model
has vision.** From that document's §1 model table: **`deepseek-flash` → Vision "Yes"**;
**`deepseek-v4-pro` → Vision "Not supported"**. So a hosted photo diagnosis must call
**`deepseek-flash`**, never `deepseek-v4-pro`.

**Hosted cost per diagnosis, computed from those cited prices and clearly labelled as
arithmetic performed here:**

Assumptions: the **documented image ceiling of 1024 tokens per image** (§4.4 of the
DeepSeek doc), plus 500 text input tokens, plus 300 output tokens. Prices: input
cache-miss $0.30/1M and output $1.20/1M at peak for `deepseek-flash`, halved off-peak
(§2.1).

```
input  = 1024 + 500 = 1524 tokens
peak:     (1524 × $0.30 + 300 × $1.20) / 1e6 = $0.0008172   per diagnosis (provider cost)
off-peak:                                    = $0.0004086   per diagnosis (provider cost)

sell price at K = 3.83 (60 % margin target, unverified granting, §6.4):
peak:     $0.00313      off-peak: $0.00156      per diagnosis
```

At peak that is roughly **$0.82 of provider cost per 1,000 diagnoses**, or **$3.13 of
sale price per 1,000** at `K = 3.83`. **If the app is popular, that is a running bill; if
it is not, the server is fixed cost for nothing.** Add the operational requirements the
DeepSeek doc already established: the proxy is *contractually required* (`ToS §2.2`)
because an APK cannot hold the key, the prepaid balance is a business-critical single
point of failure (`402`), and purchase-token verification is what moves `K` from 3.83
to 3.09 (§6.4).

| Dimension | **On-device** (LiteRT + bundled model) | **Hosted** (DeepSeek `deepseek-flash` vision) |
|---|---|---|
| **Accuracy — lab benchmark** | 98.58 %–99.92 % **as reported by sources**, on PlantVillage-class data (§2.3, §2.4). Never verified by us | No accuracy figure found. The DeepSeek doc reports no vision benchmark for plant disease. **UNVERIFIED** |
| **Accuracy — real field photos** | **31.40 % / 31.69 %** (dataset authors, on web images); **32.05 %** (Frontiers 2026, PlantVillage→PlantDoc). §6 | **UNVERIFIED** — no measurement found in either direction. This is the decisive unknown |
| **Cost per diagnosis** | $0 marginal. Cost is one-off: data, training, and a larger APK | **$0.000817** provider cost at peak (~$0.000409 off-peak); **$0.00313** sale price at `K = 3.83`. Recurring, forever |
| **Works offline** | **Yes** — the whole point. A field app with no signal still works | **No.** Requires connectivity; the DeepSeek doc documents keep-alive padding and a 10-minute pre-inference close that the client must handle (§4.3) |
| **Latency** | **5.58 ms** (GreenHealer, CPU) to **78 ms** (ParaLeafNet, Snapdragon 865); "sub-10 ms on edge hardware". **Real in-app latency UNVERIFIED** | Network round trip + inference. No latency figure was measured in this session. DeepSeek does not publish a total-duration ceiling (that doc's UNVERIFIED #6) |
| **App size** | Model **613 KB–3.0 MB** (typical) or 14.2 MB; **plus** an ~11.8 MB multi-ABI LiteRT AAR. Installed delta **UNVERIFIED** (§3.4) | Model bytes: none. An HTTP client only — *if* the key is genuinely absent, which §5.1 of the DeepSeek doc says it must be |
| **Server burden** | **None.** No proxy, no key, no balance to top up, no 429 handling, no low-balance alerting | **A server is mandatory**, two forced jobs on it: hold the key (contractual, `ToS §2.2`) and verify purchase tokens + own the credit balance (that doc §5.3) |
| **Compliance / privacy effort** | No user image leaves the device → simplest Data safety story. **The real exposure is the model licence**, not privacy (§1.4) | DeepSeek `ToS §3.3` (privacy notice + consent before first inference, DeepSeek named as sub-processor), `§8.1` (AI disclosure), `§5` (no brand use). Plus Play AI-content declarations. **But no share-alike licence risk** — `§4.2` assigns output rights to us |
| **Crop coverage** | Bounded by the training data: **tomato ✅, pepper ⚠️ (2 classes), cucumber ❌** (§1.3) | Not training-bounded — a vision model can be asked about any crop. Whether the answer is *right* is **UNVERIFIED** and unmeasured |

---

## 5. Recommendation

### 5.1 The recommendation

**Choose ON-DEVICE — but change the product, and treat the licence and the accuracy as
unresolved gates, not as details to fix later.**

Confidence: **MEDIUM** on the architecture. **LOW** on diagnostic quality.

Why on-device wins on the evidence that exists:

1. **It is the only path this factory can actually ship today.** `AGENTS.md` describes a
   Kotlin/Compose APK factory with no server component; the DeepSeek research already
   concluded the hosted path "forces a server component this factory has never built",
   and that the proxy is *contractually* required. On-device is a normal slot.
2. **Marginal cost is zero and it works offline.** For an edible-garden user standing in
   a garden, offline is not a nicety — it is the product. Hosted adds cost per diagnosis
   (`$0.000817` provider, `$0.00313` sale price at `K = 3.83`) and a prepaid balance that
   can empty (`402`).
3. **The hosted side has no accuracy evidence at all.** Nothing found in this session
   measured a vision LLM on plant-disease photos. Choosing hosted would be choosing a
   server, a key, a recurring bill, and a privacy/consent surface in exchange for an
   **unmeasured** quality difference.
4. **Google's Apache-2.0 CropNet feature vector gives a compliant backbone**, so the
   parts that can be licence-clean, are.

**But three scope changes are part of the recommendation, not optional extras:**

- **Drop cucumber, or do not claim it.** It has no training data in either confirmed
  dataset (§1.3). Shipping a cucumber diagnosis would be inventing a capability.
- **Treat pepper as one disease at most.** Two classes is not a diagnostic feature.
- **Do not phrase output as a spray instruction.** See §6. Call it a *suggestion to
  verify*, show the confidence, and say when the model is uncertain.

And the honest alternative, which the evidence also supports:

> **Do not build the diagnostic classifier yet.** Build the non-diagnostic parts of the
> product (garden log, crop calendar, symptom-checklist triage, care guidance) on-device
> with no model at all, and spend the model effort on the §5.3 measurement first. This is
> a legitimate answer to the fork: the *architecture* question (on-device vs hosted) is
> answerable now, and the answer is on-device; the *diagnosis* question is not answerable
> from the sources that exist.

### 5.2 What would invalidate this recommendation

- **A field-accuracy result that is good.** If a fine-tuned CropNet-based model, or
  `deepseek-flash` vision, scores acceptably on real phone photos (§5.3), the accuracy
  objection dissolves and on-device only gets stronger.
- **A licence ruling that the share-alike does not reach a shipped model.** Then
  PlantVillage's 38 classes become usable and tomato becomes a much better product.
- **A licence ruling that it *does* reach the model, with no acceptable workaround.**
  Then the training data must be PlantDoc (CC BY 4.0, 2.6k images) or newly collected,
  and the accuracy outlook gets *worse*, not better.
- **A crop-coverage decision by the human**: if the target really is cucumbers, no
  on-device classifier can be honestly built from these datasets and the fork must be
  re-opened.

### 5.3 The one measurement that would settle it

> **Evaluate both candidates on the same held-out set of real phone photographs of real
> leaves, taken by the target user, for the target crops — not on PlantVillage.**

Concretely: collect a few hundred field photos per crop (garden phone photos, real
backgrounds, real lighting), have them labelled by someone competent, then score
(a) a CropNet-backbone TFLite model fine-tuned on PlantVillage/PlantDoc and (b)
`deepseek-flash` vision on the same set, reporting **accuracy, top-3 accuracy, and
selective accuracy at the confidence threshold the app would actually use**.

Nothing found in this session did that for either candidate. **Until it is done, every
"99 %" in this document is a laboratory number and every hosted number is unknown.**
This measurement is cheap relative to building either product, and it decides the fork.

**Confidence in this recommendation: MEDIUM for the architecture, LOW for the
diagnostic claim — and the diagnostic claim is what the product is.**

---

## 6. Accuracy honesty

**The feature being contemplated tells a grower what to spray on food.** That raises the
bar above "the benchmark looks good". Here is what is and is not knowable.

### 6.1 What the sources actually report

| Claim | Value | Source (read 2026-09-23) |
|---|---|---|
| PlantVillage held-out test accuracy | **99.35 %** | Mohanty et al. 2016, <https://www.frontiersin.org/journals/plant-science/articles/10.3389/fpls.2016.01419/full> |
| Same models on real-world web images | **31.40 %** (dataset 1, 121 images) and **31.69 %** (dataset 2, 119 images) | Same paper |
| Random-guess baseline (38 classes) | 2.63 % | Same paper |
| Correct class in top-5 | 52.89 % / 65.61 % | Same paper |
| **Even when the crop is given to the model** | **47.8 %** (dataset 1: 33 classes / 9 crops; random guessing 22.5 %) | Same paper |
| PlantVillage → PlantDoc transfer | **99.73 % → 32.05 %**, a **67.7 pp drop**, with **mean predicted confidence still 79.76 %** | <https://www.frontiersin.org/journals/plant-science/articles/10.3389/fpls.2026.1826962/full> |
| Same study, repeated splits | mean **32.10 %** (95 % CI [31.88 %, 32.32 %]), macro-F1 **0.2859** | Same study |
| Prior literature range | "accuracy drops of **20 to 50 percentage points**" | Same study |
| A better-reported cross-domain result, best case found | 67.45 % on PlantDoc (ternary ensemble, PV + EdenLibrary merged); 91.47 % ConvNeXt in a hybrid framework | <https://onlinelibrary.wiley.com/doi/full/10.1111/jph.70264>, <https://doi.org/10.5109/7395548> |

**The single most important sentence in this document**, from the 2026 Frontiers study:

> "The simultaneous occurrence of near-random accuracy and high confidence is the
> defining feature of the reliability gap."

That is the failure mode to fear: an app that is **wrong and confident**. The 79.76 %
mean confidence at 32.05 % accuracy is the direct answer to "can't we just show a
confidence score and let the user decide?" — **no, not without calibration**, and the
same paper's mitigations (temperature scaling, selective prediction) were tested and
found to have "limited efficacy".

### 6.2 What is knowable, and what is not

**Knowable from sources:**

- Reported **benchmark** accuracy on PlantVillage-class data: ~98.6 %–99.9 %
  (reported by each source; §2.3–2.4).
- That **benchmark accuracy does not survive the move to field photos**, measured twice
  independently, with the dataset authors' own paper being the first measurement.
- That the drop is large — **20 to 50 percentage points** by one study's literature
  review, and 67.7 points in its own experiment.
- That **confidence is not a reliable proxy for correctness** in the cross-domain case.

**Not knowable from sources — mark these UNVERIFIED:**

- The accuracy of **any** of these models on **a phone photo of a real tomato leaf in a
  real garden**. Nobody in this session measured it. It is the only number that matters
  for the product.
- The accuracy of **`deepseek-flash` vision** on plant disease. No benchmark found.
- Whether **any on-device model** distinguishes tomato early blight from Septoria leaf
  spot *early*, when symptoms are subtle — disease-stage sensitivity was not measured
  anywhere in the sources read.
- Whether the models are **calibrated** in the field enough for a "confidence" UI.
- The effect of the **leaf-leakage split** on any published number (§1.1).

### 6.3 The statement the product must carry

1. **A reported benchmark accuracy is not field accuracy on a phone photo of a real
   leaf.** The PlantVillage authors reported 99.35 % and then measured 31.40 % on
   real-world images with their own model. Any marketing or store copy that says
   "99 % accurate" on the strength of a PlantVillage number would be **materially
   misleading to the user** and a Play claim that does not describe the shipped app's
   behaviour.
2. **The model cannot know what it has never seen.** If it was trained on 14 crops and 38
   classes, a cucumber leaf, a new cultivar, a nutrient deficiency, or a herbicide burn
   is **out of distribution** — and the AgriVision card's own limitation list says exactly
   this: unseen conditions "can reduce performance". GreenHealer's says: "images outside
   this scope may produce **unreliable or overconfident** predictions."
3. **The honest product is a triage aid, not an authority.** Show the candidate classes
   and the confidence, say that a photo is not a laboratory test, name the classes the
   model knows, and refuse to answer outside them. Both third-party cards in §2.3
   explicitly disclaim treatment recommendations — a shipped product should at minimum
   match the model author's own disclaimer, and arguably go further given §1.3.
4. **Do not delegate the warning to the model.** The disclaimers in §2.3 come from
   publishers who never shipped anything. The obligation here is ours.

---

## 7. UNVERIFIED register

| # | Item | Status |
|---|---|---|
| 1 | Whether CC BY-SA 3.0's "algorithms trained on the data fall under the same license" clause legally obliges a **shipped** proprietary model to be share-alike. The clause was read; its legal effect on an APK is a counsel question. | UNVERIFIED — human/counsel decision |
| 2 | PlantVillage's licence **from a licence file**. The GitHub repo has **no LICENSE file** (`"license": null` via the GitHub API); the CC BY-SA 3.0 term comes from the authors' paper prose and a third-party challenge page. | Licence stated in prose only |
| 3 | The licence of the **Mendeley** dataset `tywbtsjrjv/1` (39 classes / 61,486 augmented images) that TFDS cites as the dataset URL. No licence was shown on the page as read. | UNVERIFIED |
| 4 | Whether the **CC0-1.0** declaration on `geraldmc/plantvillage-full` is a valid relicensing. A third party cannot relicense CC BY-SA material as CC0, so the declaration is not credible — but no legal ruling was read. | UNVERIFIED — treat as unreliable |
| 5 | Exact image count: sources say **54,303 / 54,306 / 54,309**. No single authoritative figure. | Discrepancy between sources |
| 6 | **CropNet TFLite artefact size.** The model page showed `saved_model.pb` at 1.52 MB (a SavedModel, not a flatbuffer); a TFLite figure was not stated unambiguously. | UNVERIFIED |
| 7 | **Any accuracy figure for `deepseek-flash` vision on plant disease.** No benchmark found in either research session. | UNVERIFIED — the decisive gap |
| 8 | **Accuracy of any on-device model on real phone photos of real leaves.** Only PlantDoc-transfer figures exist, and those are not the product's distribution. | UNVERIFIED — the decisive gap |
| 9 | **LiteRT 2.2.0's 16 KB alignment across all artefacts.** Google's claims concern 1.4.0 / 2.0.1-alpha; one 1.4.0 companion library was reported 4 KB-aligned. No AAR was inspected and **Gradle was not run**. | UNVERIFIED — must be checked on the first bundling slot |
| 10 | **Real installed app-size delta** from adding LiteRT + a model. AAR sizes are exact (11,761,541 B for `litert:2.2.0`); the post-ABI-split, post-R8, post-linker increase is not measured. | UNVERIFIED — needs a build |
| 11 | Sizes of `litert-api:2.1.5`, `litert-gpu:2.1.5`, `litert-gpu:2.2.0` — returned **404** from the Google Maven path used. `litert-api:2.2.0` = 934,666 B. | Not found / not confirmed |
| 12 | Whether any **pure-JVM** engine (Deep Netts, SKaiNET, other) can run a MobileNet-class CNN on Android with acceptable latency and output parity. Libraries exist; capability was not tested. | UNVERIFIED |
| 13 | Whether AgriVision's `.tflite` files — published for **Raspberry Pi + Coral** — run correctly under **LiteRT on Android**. Plausible (same flatbuffer format); untested and not the publisher's target. | INFERRED, not verified |
| 14 | Maintenance, audit status, and long-term availability of the third-party HuggingFace/GitHub models in §2.3. | UNVERIFIED |
| 15 | The licence of any **deployable artefact** behind the ParaLeafNet and RTR_Lite_MobileNetV2 papers (§2.4). Papers were read; artefacts were not found. | UNVERIFIED |
| 16 | Disease-**stage** sensitivity (early vs late symptoms) for any model. Not measured in any source read, and it is exactly what an early-warning product needs. | UNVERIFIED |
| 17 | The **EdenLibrary** dataset licence (referenced by the Wiley study as a field-image source). | Not read |
| 18 | Whether an **official Android skill covers on-device ML / model deployment.** Checked: `.agents/skills/android-official/` contains no ML, LiteRT, TFLite, or model-deployment skill (its 10 skills are AGP 9 upgrade, Android CLI, credentials, Compose adaptive, Navigation 3, R8 analyzer, Play policy insights, Intent security, edge-to-edge, testing). The Android skills MCP returned no on-device-ML skill for this query either — closest were `appfunctions` (device-ai) and `camera1-to-camerax`. **So there is no official skill to follow; the primary sources above were used instead.** | Checked — none exists |

---

## 8. Sources read in this session (2026-09-23)

| Source | What it established |
|---|---|
| <https://arxiv.org/pdf/1511.08060> | PlantVillage: 54,309 images, 14 crop species, **CC BY-SA 3.0 with the trained-algorithm clarification** |
| <https://github.com/spMohanty/PlantVillage-Dataset> | 54,306 images, 14 species, 26 diseases; **no LICENSE file** (GitHub API `license: null`) |
| <https://api.github.com/repos/spMohanty/PlantVillage-Dataset> | `"license": null`, default branch `master`; root listing has no LICENCE file |
| <https://www.tensorflow.org/datasets/catalog/plant_village> | 54,303 images, 38 categories; cites the Mendeley derivative |
| <https://huggingface.co/datasets/mohanty/PlantVillage> | `cc-by-sa-3.0`, 3 configs, 43,596/10,709 splits, `leaf_id` leakage warning, class list |
| <https://data.mendeley.com/datasets/tywbtsjrjv/1> | The augmented derivative: **39 classes, 61,486 images**, adds `Background_without_leaves`; no licence shown |
| <https://www.aicrowd.com/challenges/plantvillage-disease-classification-challenge> | Independent restatement of CC BY-SA 3.0 + trained-algorithm clarification; 38 classes |
| <https://www.frontiersin.org/journals/plant-science/articles/10.3389/fpls.2016.01419/full> | **99.35 % held-out; 31.40 %/31.69 % real-world; 47.8 % with crop known; top-5 52.89 %/65.61 %; random 2.63 %** |
| <https://pmc.ncbi.nlm.nih.gov/articles/PMC5032846/> | Same paper via PMC |
| <https://github.com/pratikkayal/PlantDoc-Dataset> | **CC BY 4.0**, 2,598 images, 13 species, 27 classes; full `train/` class list showing pepper ×2 and **no cucumber** |
| <https://github.com/pratikkayal/PlantDoc-Dataset/blob/master/LICENSE.txt> | CC BY 4.0 text |
| <https://arxiv.org/abs/1911.10317> | PlantDoc paper: 2,598 images, 13 species, 27 classes, field conditions |
| <https://www.frontiersin.org/journals/plant-science/articles/10.3389/fpls.2026.1826962/full> | **99.73 % → 32.05 %, −67.7 pp, confidence still 79.76 %**; 20–50 pp literature range |
| <https://doi.org/10.5109/7395548> | PlantVillage 99.57 % vs PlantDoc 91.47 % (best hybrid); baseline PlantDoc 25.73–47.18 % |
| <https://onlinelibrary.wiley.com/doi/full/10.1111/jph.70264> | PV+EL ensemble: 67.45 % on PlantDoc |
| <https://tfhub.dev/google/cropnet/classifier/cassava_disease_V1/2> | **CropNet: Google, Apache-2.0**, MobileNetV3, 224×224×3→1280-dim, iNaturalist+ImageNet-21K plants; **cassava only** |
| <https://www.tensorflow.org/hub/tutorials/cropnet_on_device> | Official fine-tune → **export TFLite for Android** workflow |
| <https://www.tensorflow.org/hub/tutorials/cropnet_cassava> | CropNet cassava classes (6) |
| <https://developers.google.com/ml-kit/vision/image-labeling> | ML Kit base model: 400+ general categories; custom LiteRT models supported |
| <https://developers.google.com/ml-kit/vision/object-detection> | Coarse classifier: 5 categories incl. "plants" |
| <https://developers.google.com/ml-kit/custom-models> | Custom models replace defaults — i.e. you must supply your own |
| <https://huggingface.co/peachbotAI/agrivision-mobilenetv2-edge-tpu> | AgriVision: **Apache-2.0**, binary healthy/problem, 224×224, INT8 98.58 %, self-disclaimers |
| <https://huggingface.co/api/models/peachbotAI/agrivision-mobilenetv2-edge-tpu> | Exact file sizes: 627,400 B / 778,880 B; `license: apache-2.0` |
| <https://huggingface.co/api/datasets/geraldmc/plantvillage-full> | The re-upload's `cc0-1.0` claim |
| <https://github.com/MustafaBeratYavas/plant-disease-edge-ai-diagnosis-system> | GreenHealer: MIT code, MobileNetV3Large, 38-class, **3.0 MB**, 99.21 % self-reported |
| <https://impactfactor.org/PDF/IJDDT/16/IJDDT,Vol16,Issue15s,Article8.pdf> | ParaLeafNet: 38-class, 14.2 MB INT8, 5.8 M params, 78 ms Snapdragon 865 |
| <https://www.sciencedirect.com/science/article/pii/S2214662825000271> | RTR_Lite_MobileNetV2: **99.92 % lab / 82.00 % PlantDoc** |
| **<https://developer.android.com/guide/practices/page-sizes>** | **Official 16 KB rule**: native-code definition, Play deadline **2027-02-01**, AGP 8.5.1+/NDK r28+, the exact verification commands. *Last updated 2026-09-16* |
| <https://developers.google.com/edge/litert/android> | LiteRT: `com.google.ai.edge.litert:litert`, CompiledModel API, minSdk 23, version table |
| <https://github.com/google-ai-edge/litert> | "MUST USE Compiled Model API"; legacy Interpreter strictly deprecated; platform/accelerator matrix |
| <https://developers.google.com/edge/litert/next/gpu> | GPU accelerator is inside the Kotlin Maven package |
| <https://dl.google.com/dl/android/maven2/com/google/ai/edge/litert/litert/maven-metadata.xml> | **`<latest>2.2.0</latest>`**, lastUpdated 20260813; full version list |
| Maven/Google Maven HEAD requests | Exact AAR bytes: `litert:2.2.0` = 11,761,541; `litert:2.1.5` = 10,058,192; `litert-api:2.2.0` = 934,666 |
| <https://discuss.ai.google.dev/t/tensorflow-lite-and-16-kb-page-size-support/93052> | Migrate to LiteRT 1.4.0; TFLite 2.17.0 `.so` is 4 KB aligned (`0x1000`) |
| <https://github.com/google-ai-edge/LiteRT/issues/4286> | Google engineers verified `litert:1.4.0` `libtensorflowlite_jni.so` `p_align` = `0x4000` |
| <https://github.com/google-ai-edge/LiteRT/issues/8> | `liblitert_jni.so` in 2.0.1-alpha is 16 KB aligned |
| <https://github.com/tensorflow/tensorflow/issues/96602> | Legacy `org.tensorflow:tensorflow-lite` 2.17.0 is **not** 16 KB aligned; use LiteRT 1.4.1+ |
| <https://stackoverflow.com/questions/79786888/…> | Real Play upload failure from a 4 KB-aligned `libimage_processing_util_jni.so` in `litert-support:1.4.0` |
| <https://www.deepnetts.com/>, <https://github.com/sk-ai-net/SKaiNET> | Pure-JVM deep-learning libraries exist (capability unverified) |

**Repo sources consulted, not modified:**
[`../../AGENTS.md`](../../AGENTS.md) (toolchain table, "Defaults to suppress", store-pack
and compliance contracts), [`./2026-09-23-deepseek-api.md`](./2026-09-23-deepseek-api.md)
(§1 vision table, §2.1 prices, §2.2 peak windows, §4.3 timeouts, §4.4 image token
ceiling, §5.1–5.3 key/proxy requirement, §6.4 `K` table, §8 UNVERIFIED register),
`../../.cursor/rules/verification-discipline.mdc`,
`../../.cursor/rules/security-secrets.mdc`,
`.agents/skills/android-official/` (checked for an on-device-ML skill — none found).

---

## 9. Secret scan

Requested discipline check, performed 2026-09-23:

- **No API key, token, or secret appears in this document.** No key was read, copied, or
  transmitted during this task.
- This document creates no configuration and names no credential. If the project later
  takes the hosted path, the key belongs on a server we control and nowhere in the APK —
  per `§5.1` of [`./2026-09-23-deepseek-api.md`](./2026-09-23-deepseek-api.md) and
  [`../../.cursor/rules/security-secrets.mdc`](../../.cursor/rules/security-secrets.mdc).
- The on-device path recommended in §5 requires **no secret at all**, which is itself
  part of its case.
