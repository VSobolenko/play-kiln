#!/usr/bin/env node
/**
 * beforeMCPExecution — allow all configured servers; deny nothing by default.
 * Blocks only obviously dangerous patterns if present in tool name.
 */
const fs = require('fs')

let input = {}
try {
  input = JSON.parse(fs.readFileSync(0, 'utf8') || '{}')
} catch {}

const tool = String(input.tool_name || input.toolName || '')
const server = String(input.server || input.mcp_server || '')

// Soft deny: never allow a fictional "shell_exec_all" style if someone misconfigures
if (/destroy|wipe.?device|format.?disk/i.test(tool)) {
  process.stdout.write(
    JSON.stringify({
      permission: 'deny',
      user_message: 'Blocked dangerous MCP tool: ' + tool,
      agent_message: 'mcp-audit denied a destructive MCP tool name.'
    })
  )
  process.exit(0)
}

process.stdout.write(JSON.stringify({ permission: 'allow' }))
