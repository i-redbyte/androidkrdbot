package su.redbyte.androidkrdbot.infra.utils

import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.MessageEntity
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BotMentionTest {
    @Test
    fun findsMentionInMiddleOfMessageRegardlessOfCase() {
        val text = "Что думаешь, @CommissarBot, о революции?"
        val message = Message(
            messageId = 1,
            date = 0,
            chat = Chat(id = 1, type = "group"),
            text = text,
            entities = listOf(MessageEntity(
                type = MessageEntity.Type.MENTION,
                offset = text.indexOf('@'),
                length = "@CommissarBot".length
            ))
        )
        assertTrue(message.containsBotMention("commissarbot"))
        assertFalse(message.containsBotMention("anotherbot"))
    }
}
