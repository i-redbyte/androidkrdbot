package su.redbyte.androidkrdbot.cli.message

interface MessageListener {
    suspend fun handle(ctx: MessageContext)
}