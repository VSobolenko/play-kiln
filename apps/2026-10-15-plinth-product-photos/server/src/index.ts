import { mkdir } from 'node:fs/promises';
import { createApp } from './app.js';
import { loadConfig } from './config.js';
import { createDatabase } from './db.js';

const config = loadConfig();
await mkdir(config.dataDir, { recursive: true });
const db = createDatabase(config.databasePath);
const app = await createApp({ config, db });
const port = Number(process.env.PORT || 3000);
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('PORT must be between 1 and 65535');
await app.listen({ host: process.env.HOST || '127.0.0.1', port });
app.log.info({ address: `http://${process.env.HOST || '127.0.0.1'}:${port}` }, 'server listening');
