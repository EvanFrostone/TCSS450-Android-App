package edu.uw.chather.ui.chat;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Encapsulate chat message details
 * @author Alec Mac
 */
public final class ChatMessage implements Serializable {

    /**
     * id of message
     */
    private final int mMessageId;
    /**
     * content of message
     */
    private final String mMessage;
    private final String mSender;
    /**
     * timestamp of message
     */
    private final String mTimeStamp;

    /**
     * Generates a chat message
     * @param messageId id for message
     * @param message the message content
     * @param sender sender information
     * @param timeStamp time the message was sent
     */
    public ChatMessage(int messageId, String message, String sender, String timeStamp) {
        mMessageId = messageId;
        mMessage = message;
        mSender = sender;
        mTimeStamp = timeStamp;
    }

    /**
     * Static factory method to turn a properly formatted JSON String into a
     * ChatMessage object.
     * @param cmAsJson the String to be parsed into a ChatMessage Object.
     * @return a ChatMessage Object with the details contained in the JSON String.
     * @throws JSONException when cmAsString cannot be parsed into a ChatMessage.
     */
    public static ChatMessage createFromJsonString(final String cmAsJson) throws JSONException {
        final JSONObject msg = new JSONObject(cmAsJson);
        return new ChatMessage(msg.getInt("messageid"),
                msg.getString("message"),
                msg.getString("email"),
                msg.getString("timestamp"));
    }

    /**
     * Returns message
     * @return message
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * Gets sender email
     * @return sender information
     */
    public String getSender() {
        return mSender;
    }

    /**
     * Returns timestamp
     * @return timestamp
     */
    public String getTimeStamp() {
        return mTimeStamp;
    }

    /**
     * Returns message ID
     * @return message ID
     */
    public int getMessageId() {
        return mMessageId;
    }

    /**
     * Provides equality solely based on MessageId.
     * @param other the other object to check for equality
     * @return true if other message ID matches this message ID, false otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        boolean result = false;
        if (other instanceof ChatMessage) {
            result = mMessageId == ((ChatMessage) other).mMessageId;
        }
        return result;
    }
}
