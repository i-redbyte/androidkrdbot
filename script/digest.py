import asyncio
import sqlite3
import sys
from datetime import datetime, timedelta
from pyrogram import Client, enums

SOURCE_CHANNELS = [
    "apptractor",  # без @
    "android_guards_today",
    "android_broadcast",
    "mobileproglib",
    "droidgr",
    "applib",
    "devballet",
    "TheDailyKotlin"
]

TARGET_CHAT = "androidkd_test"

DB_FILE = "digest_seen.db"
PREVIEW_LEN = 200


def init_db():
    conn = sqlite3.connect(
        DB_FILE,
        timeout=10,
        isolation_level=None
    )
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute(
        "CREATE TABLE IF NOT EXISTS seen("
        "channel TEXT, msg_id INTEGER, PRIMARY KEY(channel,msg_id))"
    )
    return conn

async def collect_and_send(app: Client, db) -> str:
    utc_now = datetime.utcnow()
    since = utc_now - timedelta(days=1)

    lines = []
    cur = db.cursor()

    for chan in SOURCE_CHANNELS:
        async for m in app.get_chat_history(chan):
            if m.date is None or m.date < since:
                break
            content = m.text or m.caption
            if not content:
                continue
            if cur.execute(
                    "SELECT 1 FROM seen WHERE channel=? AND msg_id=?",
                    (chan, m.id)
            ).fetchone():
                continue

            preview = content.split("\n")[0][:PREVIEW_LEN].strip()
            link = f"https://t.me/{chan}/{m.id}"
            lines.append(f"• **{preview}** → [читать]({link})")
            cur.execute(
                "INSERT OR IGNORE INTO seen(channel,msg_id) VALUES(?,?)",
                (chan, m.id)
            )

    db.commit()

    if not lines:
        return "NO_NEW"

    lines.reverse()
    header = f"📱 *Разведданные — {utc_now:%d %b %Y}*\n\n"
    text = header + "\n".join(lines[:40])       # ≤ 40 пунктов
    return text

async def main():
    if len(sys.argv) != 4:
        print("usage: python digest.py <api_id> <api_hash> <session>", file=sys.stderr)
        sys.exit(1)

    api_id, api_hash, session = int(sys.argv[1]), sys.argv[2], sys.argv[3]

    db = init_db()
    try:
        async with Client(session, api_id, api_hash) as app:
            digest = await collect_and_send(app, db)
            print(digest)
    finally:
        db.close()

if __name__ == "__main__":
    asyncio.run(main())