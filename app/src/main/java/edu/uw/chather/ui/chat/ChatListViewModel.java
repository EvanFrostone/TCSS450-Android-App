package edu.uw.chather.ui.chat;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.uw.chather.R;
import edu.uw.chather.io.RequestQueueSingleton;

public class ChatListViewModel extends AndroidViewModel {

    private Map<Integer, MutableLiveData<List<Chatroom>>> mChatrooms;

    public ChatListViewModel(@NonNull Application application) {
        super(application);
        mChatrooms = new HashMap<>();
    }

    /**
     * Register as an observer to listen to a specific chat room's list of messages.
     * @param chatId the chatid of the chat to observer
     * @param owner the fragments lifecycle owner
     * @param observer the observer
     */
    public void addChatroomObserver(int chatId,
                                   @NonNull LifecycleOwner owner,
                                   @NonNull Observer<? super List<Chatroom>> observer) {
        getOrCreateMapEntry(chatId).observe(owner, observer);
    }

    /**
     * Return a reference to the List<> associated with the chat room. If the View Model does
     * not have a mapping for this chatID, it will be created.
     *
     * WARNING: While this method returns a reference to a mutable list, it should not be
     * mutated externally in client code. Use public methods available in this class as
     * needed.
     *
     * @param memberId the id of the member to retrieve chats for
     * @return a reference to the list of chatrooms
     */
    public List<Chatroom> getChatroomListByMemberId(final int memberId) {
        return getOrCreateMapEntry(memberId).getValue();
    }

    private MutableLiveData<List<Chatroom>> getOrCreateMapEntry(final int memberId) {
        if (!mChatrooms.containsKey(memberId)) {
            mChatrooms.put(memberId, new MutableLiveData<>(new ArrayList<>()));
        }
        return mChatrooms.get(memberId);
    }



    /**
     * Makes a request to the web service to get the first batch of messages for a given Chat Room.
     * Parses the response and adds the ChatMessage object to the List associated with the
     * ChatRoom. Informs observers of the update.
     *
     * Subsequent requests to the web service for a given chat room should be made from
     * getNextMessages()
     *
     * @param memberId the member id to request chatrooms of
     * @param jwt the users signed JWT
     */
    public void getChatrooms(final int memberId, final String jwt) {
        String url = getApplication().getResources().getString(R.string.base_url) +
                "chats/" + memberId;

        Request request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null, //no body for this get request
                this::handleSuccess,
                this::handleError) {

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                // add headers <key,value>
                headers.put("Authorization", jwt);
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                10_000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        //Instantiate the RequestQueue and add the request to the queue
        RequestQueueSingleton.getInstance(getApplication().getApplicationContext())
                .addToRequestQueue(request);

    }
    /**
     *
     *
     * UMMM
     */


    /**
     * When a chat message is received externally to this ViewModel, add it
     * with this method.
     * @param memberId
     * @param chatroom
     */
    public void addChatroom(final int memberId, final Chatroom chatroom) {
        List<Chatroom> list = getChatroomListByMemberId(memberId);
        list.add(chatroom);
        getOrCreateMapEntry(memberId).setValue(list);
    }

    private void handleSuccess(final JSONObject response) {
        List<Chatroom> list;
        if (!response.has("chatId")) {
            throw new IllegalStateException("Unexpected response in ChatViewModel: " + response);
        }
        try {
            list = getChatroomListByMemberId(response.getInt("memberId"));
            JSONArray chatrooms = response.getJSONArray("rows");
            for(int i = 0; i < chatrooms.length(); i++) {
                JSONObject chatroom = chatrooms.getJSONObject(i);
                Chatroom cChatroom = new Chatroom(
                        chatroom.getInt("chatid")
                );
                if (!list.contains(cChatroom)) {
                    // don't add a duplicate
                    list.add(0, cChatroom);
                } else {
                    // this shouldn't happen but could with the asynchronous
                    // nature of the application
                    Log.wtf("Chat message already received",
                            "Or duplicate id:" + cChatroom.getChatId());
                }

            }
            //inform observers of the change (setValue)
            getOrCreateMapEntry(response.getInt("messageId")).setValue(list);

        }catch (JSONException e) {
            Log.e("JSON PARSE ERROR", "Found in handle Success ChatViewModel");
            Log.e("JSON PARSE ERROR", "Error: " + e.getMessage());
        }
    }

    private void handleError(final VolleyError error) {
        if (Objects.isNull(error.networkResponse)) {
            Log.e("NETWORK ERROR", error.getMessage());
        }
        else {
            String data = new String(error.networkResponse.data, Charset.defaultCharset());
            Log.e("CLIENT ERROR",
                    error.networkResponse.statusCode +
                            " " +
                            data);
        }
    }

}