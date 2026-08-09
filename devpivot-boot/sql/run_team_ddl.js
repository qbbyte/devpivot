const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');

const SQL_FILE = path.resolve(__dirname, 'team_ddl.sql');

async function main() {
  const sql = fs.readFileSync(SQL_FILE, 'utf8');
  const conn = await mysql.createConnection({
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: '123456',
    database: 'devpivot',
    multipleStatements: true
  });
  try {
    const [results] = await conn.query(sql);
    console.log('DDL executed. statement count =', Array.isArray(results) ? results.length : 1);

    const [rows] = await conn.query(
      `select table_name from information_schema.tables
       where table_schema = 'devpivot' and table_name like 'ai_team%'
       order by table_name`
    );
    console.log('ai_team* tables now in devpivot:');
    rows.forEach(r => console.log('  -', r.TABLE_NAME || r.table_name));
  } finally {
    await conn.end();
  }
}

main().catch(e => { console.error('ERROR:', e.message); process.exit(1); });
