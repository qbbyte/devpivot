import fs from 'fs'
const raw = fs.readFileSync('D:/mine-project/devPivot/_db_doc_content.txt','utf8')
const md = raw.split('\n').slice(1).join('\n')

function extractDdlBlocks(md){if(!md)return[];const re=/```sql\s*([\s\S]*?)```/gi;const blocks=[];let m;while((m=re.exec(md))){const sql=m[1].trim();if(sql)blocks.push(sql)}return blocks}
function splitCreateTables(sql){return sql.split(';').map(s=>s.trim()).map(s=>{const idx=s.search(/CREATE\s+TABLE/i);return idx>=0?s.slice(idx):''}).filter(s=>/^CREATE\s+TABLE/i.test(s)).map(s=>s+';')}
function splitTopLevel(str,sep){const parts=[];let depth=0;let cur='';for(let i=0;i<str.length;i++){const ch=str[i];if(ch==='(')depth++;else if(ch===')')depth--;if(ch===sep&&depth===0){parts.push(cur);cur=''}else cur+=ch}if(cur.trim())parts.push(cur);return parts}
function parseColumn(line){const m=line.match(/^[`"]?([A-Za-z0-9_]+)[`"]?\s+([A-Za-z0-9_()]+)/);if(!m)return null;return {name:m[1],type:m[2]}}
function parseCreateTable(sql){
  const createMatch=sql.match(/CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`"]?([A-Za-z0-9_]+)[`"]?\s*\(/i);if(!createMatch)return null;
  const tableName=createMatch[1];
  const tblComment=sql.match(/\)[^;]*COMMENT\s*=\s*'([^']*)'/i);const tableComment=tblComment?tblComment[1]:'';
  const startIdx=sql.indexOf('(',createMatch.index)+1;let depth=1;let endIdx=startIdx;for(let i=startIdx;i<sql.length;i++){if(sql[i]==='(')depth++;else if(sql[i]===')'){depth--;if(depth===0){endIdx=i;break}}}
  const inner=sql.slice(startIdx,endIdx);const parts=splitTopLevel(inner,',');
  const columns=[];for(const raw of parts){const line=raw.trim();if(!line)continue;const upper=line.toUpperCase();if(/^(ENGINE|DEFAULT CHARSET|CHARSET|COLLATE|AUTO_INCREMENT|COMMENT)\b/.test(upper))continue;if(upper.startsWith('PRIMARY KEY'))continue;if(/^(KEY|INDEX|FULLTEXT|SPATIAL)\b/.test(upper))continue;if(upper.startsWith('UNIQUE KEY')||upper.startsWith('UNIQUE INDEX'))continue;if(upper.startsWith('CONSTRAINT')||upper.startsWith('FOREIGN KEY'))continue;const col=parseColumn(line);if(col)columns.push(col)}
  return {name:tableName,comment:tableComment,columns}
}
function parseDbDoc(md){if(!md)return[];const tables=[];extractDdlBlocks(md).forEach(b=>{splitCreateTables(b).forEach(stmt=>{const t=parseCreateTable(stmt);if(t)tables.push(t)})});return tables}

const blocks=extractDdlBlocks(md)
console.log('ddlBlocks count:', blocks.length)
const tables=parseDbDoc(md)
console.log('parsedTables count:', tables.length)
console.log('tables:', tables.map(t=>t.name+`(${t.columns.length})`).join(', '))
