import { parse, compileScript, compileTemplate } from '@vue/compiler-sfc'
import fs from 'fs'
const file = 'src/views/portal/steps/clarify.vue'
const src = fs.readFileSync(file, 'utf-8')
const { descriptor, errors } = parse(src, { filename: file })
if (errors.length) { console.error('PARSE ERRORS:', errors); process.exit(1) }
const script = compileScript(descriptor, { id: 'x' })
const tpl = compileTemplate({
  source: descriptor.template.content,
  filename: file,
  compilerOptions: { bindingMetadata: script.bindings }
})
if (tpl.errors && tpl.errors.length) {
  console.error('TEMPLATE ERRORS:')
  tpl.errors.forEach(e => console.error(' -', e.message || e))
  process.exit(1)
}
console.log('OK: clarify.vue compiles cleanly')
