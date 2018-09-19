package au.edu.unimelb.eng.navibee;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import au.edu.unimelb.eng.navibee.navigation.EventRVDivider;
import au.edu.unimelb.eng.navibee.navigation.EventRVEntry;
import au.edu.unimelb.eng.navibee.navigation.EventRVIndefiniteProgressBar;
import au.edu.unimelb.eng.navibee.navigation.EventRVItem;
import au.edu.unimelb.eng.navibee.navigation.EventsRVAdaptor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

public class EventsActivity extends AppCompatActivity {

    // Event item class
    public static class EventItem {

        private String holder;
        private String eventId;
        private String name;
        private String location;
        private Timestamp time;
        private Map<String, Boolean> users;
        private Boolean isTag = false;
        private ArrayList<String> picsStoragePath;

        public EventItem(){}

        public EventItem(String name, String holder, String location, Timestamp time, Map<String, Boolean> users, ArrayList<String> picsStoragePath){
            this.holder = holder;
            this.name = name;
            this.location = location;
            this.users = users;
            this.time = time;
            this.picsStoragePath = picsStoragePath;
        }

        public EventItem(String name, String holder, String location, Date time, Map<String, Boolean> users, ArrayList<String> picsStoragePath){
            this.holder = holder;
            this.name = name;
            this.location = location;
            this.users = users;
            this.time = new Timestamp(time);
            this.picsStoragePath = picsStoragePath;
        }

        public String getHolder() { return holder; }

        public String getName(){
            return name;
        }

        public String getLocation(){
            return location;
        }

        public Map<String, Boolean> getUsers(){
            return users;
        }

        public Boolean isTag() {
            return isTag;
        }

        public void setTag(Boolean isTag){
            this.isTag = isTag;
        }

        public String getEventId(){
            return eventId;
        }

        public void setEventId(String eventId){
            this.eventId = eventId;
        }

        public Timestamp getTime() { return time; }

        public ArrayList<String> getPicsStoragePath() {
            ArrayList<String> result = new ArrayList<>();
            result.addAll(picsStoragePath);
            return result;
        }

        @Exclude
        public Date getTime_() { return time.toDate(); }
    }

    private String uid;
    private FirebaseFirestore db;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter viewAdapter;
    private RecyclerView.LayoutManager viewManager;

    private ArrayList<EventItem> eventList;
    private ArrayList<EventRVItem> events = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        // loading
        events.add(new EventRVIndefiniteProgressBar());

        // set up recycler view
        recyclerView = (RecyclerView) findViewById(R.id.events_recycler_view);
        viewManager = new LinearLayoutManager(this);
        viewAdapter = new EventsRVAdaptor(events);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(viewManager);
        recyclerView.setAdapter(viewAdapter);
//        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL));

        loadEvents();

    }

    @Override
    public void onRestart() {
        super.onRestart();
        loadEvents();
    }

    private void loadEvents() {
        eventList = new ArrayList<>();

        db.collection("events").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        EventItem eventItem = document.toObject(EventItem.class);
                        eventItem.setEventId(document.getId());
                        eventList.add(eventItem);
                    }

                } else {
                    // fail to pull data
                }
                updateRecyclerVIew();
            }
        });
    }

    private void updateRecyclerVIew() {
        ArrayList<EventItem> organisedList = new ArrayList<>();
        ArrayList<EventItem> joinedList = new ArrayList<>();
        ArrayList<EventItem> recommendedList = new ArrayList<>();

        for (EventItem i : eventList) {
            if (i.getHolder().equals(uid)) {
                organisedList.add(i);
            }
            if ((!i.getHolder().equals(uid)) && i.getUsers().keySet().contains(uid)) {
                joinedList.add(i);
            }
            if (!i.getUsers().keySet().contains(uid)) {
                recommendedList.add(i);
            }
        }

        events.clear();
        // Organised event list
        if (!organisedList.isEmpty()) {
            events.add(new EventRVDivider(getResources().getString(R.string.event_organised_events)));
            addToEntry(organisedList);
        }

        // Joined event list
        if (!joinedList.isEmpty()) {
            events.add(new EventRVDivider(getResources().getString(R.string.event_joined_events)));
            addToEntry(joinedList);
        }

        // Recommended event list
        if (!recommendedList.isEmpty()) {
            events.add(new EventRVDivider(getResources().getString(R.string.event_recommended_events)));
            addToEntry(recommendedList);
        }

        viewAdapter.notifyDataSetChanged();
    }

    private void addToEntry(ArrayList<EventItem> list) {
        for (EventItem i : list) {
            events.add(new EventRVEntry(
                    i.getName(),
                    i.getLocation(),
                    view -> {
                        String relationship;
                        if(i.getHolder().equals(uid)) {
                            relationship = "holder";
                        }
                        else if(i.getUsers().keySet().contains(uid)){
                            relationship = "participant";
                        }
                        else {
                            relationship = "passerby";
                        }

                        Intent intent = new Intent(EventsActivity.this, EventDetailsActivity.class);
                        intent.putExtra("eventId", i.getEventId());
                        intent.putExtra("relationship", relationship);
                        startActivity(intent);
                    }
            ));
        }
    }

    public void selectFriends(View view) {
        startActivity(new Intent(this, EventEditActivity.class));
    }

}
