const mysql = require('mysql2/promise');

async function main() {
  const conn = await mysql.createConnection({
    host: 'localhost', port: 3306, user: 'root', password: '123456', database: 'devpivot'
  });
  try {
    const [rows] = await conn.query(
      `select user_id, user_name, nick_name, avatar, length(avatar) as av_len
       from sys_user where status='0' and del_flag='0' limit 10`
    );
    console.log('sys_user.avatar sample:');
    rows.forEach(r => console.log(`  uid=${r.user_id} name=${r.user_name} nick=${r.nick_name} avatar=${JSON.stringify(r.avatar)} len=${r.av_len}`));
  } finally {
    await conn.end();
  }
}
main().catch(e => { console.error('ERROR:', e.message); process.exit(1); });
