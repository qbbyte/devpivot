import { parse } from '@vue/compiler-sfc'
import { readFileSync } from 'node:fs'

const files = [
  'src/views/portal/steps/proto.vue',
  'src/views/portal/steps/tech.vue',
  'src/views/portal/steps/db.vue',
  'src/views/portal/steps/clarify.vue'
]

let failed = false
for (const f of files) {
  try {
    const src = readFileSync(f, 'utf-8')
    const { descriptor, errors } = parse(src, { filename: f })
    if (errors && errors.length) {
      failed = true
      console.log(`✗ ${f} PARSE ERRORS:`)
      errors.forEach(e => console.log('   ', e.message))
      continue
    }
    // 校验 template 与 script 块是否能编译（仅语法层面）
    const hasTemplate = !!descriptor.template
    const hasScript = !!descriptor.scriptSetup || !!descriptor.script
    if (!hasTemplate || !hasScript) {
      failed = true
      console.log(`✗ ${f} 缺少必要块: template=${hasTemplate} script=${hasScript}`)
      continue
    }
    console.log(`✓ ${f} OK (template+script parsed)`)
  } catch (e) {
    failed = true
    console.log(`✗ ${f} EXCEPTION: ${e.message}`)
  }
}
process.exit(failed ? 1 : 0)
