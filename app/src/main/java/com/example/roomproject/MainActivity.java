package com.example.roomproject;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.roomproject.models.Building;
import com.example.roomproject.models.Room;
import com.example.roomproject.models.Light;
import com.example.roomproject.models.Status;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private HistoryAdapter mAdapter;

    public Activity context;
    public List<Building> buildings;
    public Building building;
    public List<Room> rooms;
    public List<Room> roomsSelected;
    public Room room;
    public List<Light> lights;
    public List<Light> lightsSelected;
    public Light light;
    public List<String> colors;
    public String color;
    public ImageButton imageButtonLight;
    public Button buttonRefresh;
    public ScrollView scrollView;
    public ProgressBar progressBar;
    public Spinner spinnerBuilding;
    public Spinner spinnerRoom;
    public Spinner spinnerLight;
    public Spinner spinnerColor;
    public Response.Listener<JSONArray> responseBuildings;
    public Response.Listener<JSONArray> responseRoom;
    public Response.Listener<JSONArray> responseLight;
    public Response.ErrorListener errorListener;
    public String urlApiRooms = "https://faircorp-benjamin.cleverapps.io/api/rooms";
    public String urlApiBuildings = "https://faircorp-benjamin.cleverapps.io/api/buildings";
    public String urlApiLights = "https://faircorp-benjamin.cleverapps.io/api/lights";
    private RequestQueue requestQueue;
    MqttAndroidClient mqttAndroidClient;
    public View view;

    final String serverUri = "ssl://m21.cloudmqtt.com:26964";
    final String clientId = MqttClient.generateClientId();
    final String subscriptionTopic = "order";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_context_management);

        MainActivity.this.colors = new ArrayList<>();
        colors.add("bleu");
        colors.add("vert");
        colors.add("jaune");
        colors.add("rouge");
        imageButtonLight = findViewById(R.id.imageButtonLight);
        buttonRefresh = findViewById(R.id.buttonRef);
        scrollView = findViewById(R.id.scrollView);
        progressBar = findViewById(R.id.progressBar);
        spinnerBuilding = findViewById(R.id.spinnerBuilding);
        spinnerRoom = findViewById(R.id.spinnerRoom);
        spinnerLight = findViewById(R.id.spinnerLight);
        spinnerColor = findViewById(R.id.spinnerColor);

        imageButtonLight.setOnClickListener(new AdapterView.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.switchLight(MainActivity.this.light);
            }
        });

        spinnerBuilding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                MainActivity.this.building = MainActivity.this.buildings.get(i);
                MainActivity.this.roomsSelected = new ArrayList<>();
                for (int j = 0 ; j<MainActivity.this.rooms.size(); j++) {
                    if (MainActivity.this.rooms.get(j).getBuildingId().equals(building.getId())) {
                        MainActivity.this.roomsSelected.add(MainActivity.this.rooms.get(j));
                    }
                }
                setSpinnerRoom();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spinnerRoom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                MainActivity.this.room = MainActivity.this.roomsSelected.get(i);
                MainActivity.this.lightsSelected = new ArrayList<>();
                for (int j = 0 ; j<MainActivity.this.lights.size(); j++) {
                    if (MainActivity.this.lights.get(j).getRoomId().equals(room.getId())) {
                        MainActivity.this.lightsSelected.add(MainActivity.this.lights.get(j));
                    }
                }
                setSpinnerLight();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spinnerLight.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                MainActivity.this.light = MainActivity.this.lightsSelected.get(i);
                setSpinnerColor();
                set();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        spinnerColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
                if (!(MainActivity.this.colors.get(i).equals(MainActivity.this.color))) {
                    MainActivity.this.color = MainActivity.this.colors.get(i);
                    MainActivity.this.switchColor(MainActivity.this.light,MainActivity.this.color);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        responseRoom = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    MainActivity.this.rooms = new ArrayList<>();
                    for (int i = 0; i< response.length(); i++) {
                        Room room = new Room();
                        JSONObject roomjson = response.getJSONObject(i);
                        room.setId(Long.parseLong(roomjson.getString("id")));
                        room.setName(roomjson.getString("name"));
                        room.setFloor(Long.parseLong(roomjson.getString("floor")));
                        room.setBuildingId(Long.parseLong(roomjson.getString("buildingId")));
                        String room_status = roomjson.getString("status");
                        if (room_status.equals("ON")) {
                            room.setStatus(Status.ON);
                        }
                        else{
                            room.setStatus(Status.OFF);
                        }
                        rooms.add(room);
                    }
                }
                catch ( JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        responseLight = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    MainActivity.this.lights = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {

                        Light light = new Light();
                        JSONObject lightjson = response.getJSONObject(i);
                        light.setId(Long.parseLong(lightjson.getString("id")));
                        light.setLevel(Integer.parseInt(lightjson.getString("level")));
                        light.setRoomId(Long.parseLong(lightjson.getString("roomId")));
                        light.setColor(lightjson.getString("color"));
                        light.setBrightness(lightjson.getString("brightness"));

                        String light_status = lightjson.getString("status");

                        if (light_status.equals("ON")) {
                            light.setStatus(Status.ON);
                        } else {
                            light.setStatus(Status.OFF);
                        }
                        lights.add(light);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        responseRoom = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    MainActivity.this.rooms = new ArrayList<>();
                    for (int i =0 ; i < response.length(); i++) {
                        Room room = new Room();
                        JSONObject roomjson = response.getJSONObject(i);
                        room.setId(Long.parseLong(roomjson.getString("id")));
                        room.setName(roomjson.getString("name"));
                        room.setFloor(Long.parseLong(roomjson.getString("floor")));
                        room.setBuildingId(Long.parseLong(roomjson.getString("buildingId")));

                        String room_status = roomjson.getString("status");

                        if (room_status.equals("ON")) {
                            room.setStatus(Status.ON);
                        } else {
                            room.setStatus(Status.OFF);
                        }
                        rooms.add(room);
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        responseBuildings = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    MainActivity.this.buildings = new ArrayList<>();
                    for (int i = 0 ; i < response.length() ; i ++) {
                        Building building = new Building();
                        JSONObject buildingjson = response.getJSONObject(i);
                        building.setId(Long.parseLong(buildingjson.getString("id")));
                        building.setName(buildingjson.getString("name"));
                        buildings.add(building);
                    }
                    MainActivity.this.setSpinnerBuilding();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        };

        errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        };

        requestQueue = Volley.newRequestQueue(this);

        progressBar.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.INVISIBLE);
        getBuildings();
        getRooms();
        getLights();


        mAdapter = new HistoryAdapter(new ArrayList<String>());

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    addToHistory("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();
                } else {
                    addToHistory("Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                addToHistory("The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                addToHistory("Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName("zvafqgcd");
        mqttConnectOptions.setPassword("HgxcwISOxy_z".toCharArray());







        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to connect to: " + serverUri);
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    private void setSpinnerBuilding() {
        List<String> buildings_names = new ArrayList<>();
        for (Building building : buildings) {
            buildings_names.add(building.getName());
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, buildings_names);
        spinnerBuilding.setAdapter(arrayAdapter);
    }

    private void setSpinnerRoom() {
        List<String> ids = new ArrayList<>();
        for (Room room : roomsSelected) {
            ids.add(room.getName());
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,R.layout.spinner_item, ids);
        spinnerRoom.setAdapter(arrayAdapter);

    }

    private void setSpinnerLight() {
        List<Long> lights_id = new ArrayList<>();
        for (Light light : lightsSelected) {
            lights_id.add(light.getId());
        }
        ArrayAdapter<Long> arrayAdapter = new ArrayAdapter<>(this,R.layout.spinner_item, lights_id);
        spinnerLight.setAdapter(arrayAdapter);

    }

    private void setSpinnerColor() {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,R.layout.spinner_item, MainActivity.this.colors);
        spinnerColor.setAdapter(arrayAdapter);
    }

    public void set() {
        if (light.getStatus().equals(Status.ON)) {
            imageButtonLight.setImageResource(R.drawable.ic_bulb_on);
        } else {
            imageButtonLight.setImageResource(R.drawable.ic_bulb_off);
        }
        progressBar.setVisibility(View.INVISIBLE);
        scrollView.setVisibility(View.VISIBLE);
    }

    public void switchLight(Light light) {
        if(light.getStatus().equals(Status.ON)) {
            light.setStatus(Status.OFF);
        } else {
            light.setStatus(Status.ON);
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, urlApiLights +"/" + light.getId() + "/switch", null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String on;
                        try {
                            on = response.getString("status");
                            if(on.equalsIgnoreCase("ON")) {
                                MainActivity.this.light.setStatus(Status.ON);
                            } else {
                                MainActivity.this.light.setStatus(Status.OFF);
                            }
                            MainActivity.this.set();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, errorListener);
        requestQueue.add(jsonObjectRequest);
        progressBar.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.INVISIBLE);
    }


    void switchColor(Light light, String color) {
        int index = MainActivity.this.colors.indexOf(color);
        light.setColor(color);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, urlApiLights +"/" +light.getId().toString() + "/changeColor/" + light.getColor(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String on;
                        try {
                            on = response.getString("color");
                            MainActivity.this.color = on;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, errorListener);
        requestQueue.add(jsonObjectRequest);
        spinnerColor.setSelection(index);
        progressBar.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.INVISIBLE);
    }

    public void getBuildings() {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, urlApiBuildings, null, responseBuildings, errorListener);
        requestQueue.add(jsonArrayRequest);
    }

    public void getRooms() {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, urlApiRooms, null, responseRoom,errorListener);
        requestQueue.add(jsonArrayRequest);
    }

    public void getLights() {
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, urlApiLights, null, responseLight, errorListener);
        requestQueue.add(jsonArrayRequest);
    }


    public void refresh(View view) {
        progressBar.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.INVISIBLE);
        getBuildings();
    }


    private void addToHistory(String mainText){
        System.out.println("LOG: " + mainText);
        mAdapter.add(mainText);
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();

    }

    public void subscribeToTopic(){
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    addToHistory("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to subscribe");
                }
            });

            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // message Arrived!
                    String[] messagesepareted = message.toString().split(" ");
                    Light light = MainActivity.this.lights.get(Integer.parseInt(messagesepareted[0])-1);
                    if (messagesepareted[1].contains("Switch")) {
                        if (!(messagesepareted[2].contains(light.getStatus().toString()))) {
                            switchLight(light);
                        }
                    }
                    else if (messagesepareted[1].contains("ChangeColor")) {
                        if (!(messagesepareted[2].contains(light.getColor()))) {
                            switchColor(light,messagesepareted[2]);
                        }
                    }
                }
            });

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    }
