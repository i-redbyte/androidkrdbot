import sys
import json
import asyncio
from pyrogram import Client

chat_username = "androidkrd"

async def get_members(api_id: int, api_hash: str):
    # print("Starting Pyrogram client...", file=sys.stderr)
    app = Client("bot_auth", api_id=api_id, api_hash=api_hash)
    async with app:
        try:
            chat = await app.get_chat(chat_username)
            members = []
            async for member in app.get_chat_members(chat.id):
                user = member.user
                members.append({
                    "id": user.id,
                    "name": user.first_name or "",
                    "userName": user.username or ""
                })
            print(json.dumps(members))  # to stdout
        except Exception as e:
            print(f"ERROR: {e}", file=sys.stderr)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python members_exporter.py <api_id> <api_hash>", file=sys.stderr)
        sys.exit(1)

    try:
        api_id = int(sys.argv[1])
        api_hash = sys.argv[2]
        asyncio.run(get_members(api_id, api_hash))
    except Exception as e:
        print(f"FATAL: {e}", file=sys.stderr)
        sys.exit(2)
