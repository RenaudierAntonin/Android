package com.example.roomproject;

import android.annotation.SuppressLint;
import android.app.Activity; //Import des librairies servant à l'aspect graphique de l'application
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

import com.android.volley.Request; //Import des librairies permettant de faire des requetes HTML et de gérer le JSON
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.roomproject.models.Building; //Import des modèles utilisés par l'application
import com.example.roomproject.models.Room;
import com.example.roomproject.models.Light;
import com.example.roomproject.models.Status;

import org.eclipse.paho.client.mqttv3.MqttClient; //Import des librairies permettant l'utilisation du MQTT
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


    private HistoryAdapter mAdapter; //Objet permettant de gérer l'historique des requêtes mqtt

    public Activity context; //Contexte de l'activité

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
    public String Text = "Actual Color : ";


    //Objet en lien avec l'aspect graphique de l'application
    public ImageButton imageButtonLight; //Image de l'état des lights
    public Button buttonRefresh; //Bouton
    public ScrollView scrollView;
    public ProgressBar progressBar;
    //Liste déroulante
    public Spinner spinnerBuilding;
    public Spinner spinnerRoom;
    public Spinner spinnerLight;
    public Spinner spinnerColor;

    //Gestion des requêtes html et des JSON
    public Response.Listener<JSONArray> responseBuildings;
    public Response.Listener<JSONArray> responseRoom;
    public Response.Listener<JSONArray> responseLight;
    public Response.ErrorListener errorListener;
    public String urlApiRooms = "https://faircorp-benjamin.cleverapps.io/api/rooms";
    public String urlApiBuildings = "https://faircorp-benjamin.cleverapps.io/api/buildings";
    public String urlApiLights = "https://faircorp-benjamin.cleverapps.io/api/lights";
    private RequestQueue requestQueue;

    public View view;

    //Objet permettant le fonctionnement du mqtt
    private MqttAndroidClient mqttAndroidClient;
    private final String serverUri = "ssl://m21.cloudmqtt.com:26964";
    private final String clientId = MqttClient.generateClientId();
    final String subscriptionTopic = "order";


    //Main de l'application Android
    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Initialisation des différents objets
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_context_management);

        MainActivity.this.colors = new ArrayList<>();
        colors.add("bleu");
        colors.add("vert");
        colors.add("jaune");
        colors.add("rouge");

        //On lie l'objet java à l'objet xml
        imageButtonLight = findViewById(R.id.imageButtonLight);
        buttonRefresh = findViewById(R.id.buttonRef);
        scrollView = findViewById(R.id.scrollView);
        progressBar = findViewById(R.id.progressBar);
        spinnerBuilding = findViewById(R.id.spinnerBuilding);
        spinnerRoom = findViewById(R.id.spinnerRoom);
        spinnerLight = findViewById(R.id.spinnerLight);
        spinnerColor = findViewById(R.id.spinnerColor);

        progressBar.setVisibility(View.INVISIBLE);
        scrollView.setVisibility(View.VISIBLE);

        //Fonction permettant de changer l'état de la light quand on clique sur l'image
        imageButtonLight.setOnClickListener(new AdapterView.OnClickListener() { //On agit quand on clique sur l'objet imageButtonLight
            @Override
            public void onClick(View view) {
                MainActivity.this.switchLightOnClick(MainActivity.this.light); //On appelle la fonction switchLight qui change l'état de la lampe
            }                                                           // et qui fait aussi la requête afin de changer en base de données
        });

        //Fonction définissant le comportement à adopter quand on choisit un batiment dans le spinner
        spinnerBuilding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) { //Le parametre i correspond à l'id du batiment parmi tous les batiments
                MainActivity.this.building = MainActivity.this.buildings.get(i); // On change le building sélectionné
                MainActivity.this.roomsSelected = new ArrayList<>(); // On remet la liste des pièces à zéro
                for (int j = 0 ; j<MainActivity.this.rooms.size(); j++) {
                    if (MainActivity.this.rooms.get(j).getBuildingId().equals(building.getId())) {
                        MainActivity.this.roomsSelected.add(MainActivity.this.rooms.get(j)); //On sélectionne les pièces dans le batiment
                    }
                }
                setSpinnerRoom(); //On met à jour le spinner des pièces
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //Fonction définissant le comportement à adopter quand on choisit une pièce dans le spinner
        spinnerRoom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) { //Le parametre i correspond à l'id de la pièce parmi toutes les pièces
                MainActivity.this.room = MainActivity.this.roomsSelected.get(i); //On change la pièce sélectionnée
                MainActivity.this.lightsSelected = new ArrayList<>(); //On remet la liste des lumières à zéro
                for (int j = 0 ; j<MainActivity.this.lights.size(); j++) {
                    if (MainActivity.this.lights.get(j).getRoomId().equals(room.getId())) {
                        MainActivity.this.lightsSelected.add(MainActivity.this.lights.get(j)); //On sélectionne les lumières dans la pièce
                    }
                }
                setSpinnerLight(); //On met à jour le spinner des lumières
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //Fonction définissant le comportement à adopter quand on choisit une lumière dans le spinner
        spinnerLight.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {//Le parametre i correspond à l'di de la lampe parmi toutes les lampes
                MainActivity.this.light = MainActivity.this.lightsSelected.get(i); //On change la lampe sélectionnée
                setSpinnerColor(); //On met à jour le spinner des couleurs
                MainActivity.this.color=MainActivity.this.light.getColor();
                int index = MainActivity.this.colors.indexOf(MainActivity.this.color);
                spinnerColor.setSelection(index);
                set(); //On change l'imageButtonLight si besoin
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //Fonction définissant le comportement à adopter quand on choisit une couleur dans le spinner
        spinnerColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int i, long l) {
                if (!(MainActivity.this.color.equals(MainActivity.this.colors.get(i)))) {
                    MainActivity.this.color = MainActivity.this.colors.get(i); //On change la couleur sélectionnée
                    MainActivity.this.switchColorOnCLick(MainActivity.this.light,MainActivity.this.color);//On appelle switchColor qui permet de changer la couleur en base de donnée
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //On définit comment on réagit quand responseRoom reçoit un JSONArray
        responseRoom = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    MainActivity.this.rooms = new ArrayList<>(); //On initialise rooms
                    for (int i = 0; i< response.length(); i++) { //On parcourt tout le JSON
                        Room room = new Room(); //On crée une pièce
                        JSONObject roomjson = response.getJSONObject(i); //On récupère l'élément i du JSON
                        room.setId(Long.parseLong(roomjson.getString("id"))); //On set l'id de la pièce
                        room.setName(roomjson.getString("name")); //On set le nom de la pièce
                        room.setFloor(Long.parseLong(roomjson.getString("floor"))); //On set le niveau de la pièce
                        room.setBuildingId(Long.parseLong(roomjson.getString("buildingId"))); //On set le buildingId de la pièce
                        String room_status = roomjson.getString("status"); //On set son status
                        if (room_status.equals("ON")) {
                            room.setStatus(Status.ON);
                        }
                        else{
                            room.setStatus(Status.OFF);
                        }
                        rooms.add(room); //On ajoute la pièce à rooms
                    }
                }
                catch ( JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        //On définit comment on réagit quand responseLight reçoit un JSONArray
        responseLight = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    MainActivity.this.lights = new ArrayList<>(); //On initialise lights
                    for (int i = 0; i < response.length(); i++) { //On parcourt tout le JSON
                        Light light = new Light(); //On crée une lumière
                        JSONObject lightjson = response.getJSONObject(i);//On récupère l'élément i du JSON
                        light.setId(Long.parseLong(lightjson.getString("id"))); //On set l'id de la lumière
                        light.setLevel(Integer.parseInt(lightjson.getString("level")));//On set le level
                        light.setRoomId(Long.parseLong(lightjson.getString("roomId")));//On set le roomId
                        light.setColor(lightjson.getString("color"));//On set la couleur
                        light.setBrightness(lightjson.getString("brightness"));//On set la luminosité
                        String light_status = lightjson.getString("status");//On set le status
                        if (light_status.equals("ON")) {
                            light.setStatus(Status.ON);
                        } else {
                            light.setStatus(Status.OFF);
                        }
                        lights.add(light); //On ajoute la lumière dans lights
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        //On définit comment on réagit quand responseBuilidings reçoit un JSONArray
        responseBuildings = new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    MainActivity.this.buildings = new ArrayList<>(); //On initialise buildings
                    for (int i = 0 ; i < response.length() ; i ++) { //On parcourt tout le JSON
                        Building building = new Building(); //On crée un batiment
                        JSONObject buildingjson = response.getJSONObject(i); //On récupère l'élement i du JSON
                        building.setId(Long.parseLong(buildingjson.getString("id")));// On set l'id
                        building.setName(buildingjson.getString("name"));//On set le nom
                        buildings.add(building);//On ajoute le batiment à buildings
                    }
                    MainActivity.this.setSpinnerBuilding();//On configure le spinner des batiments
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        };

        //Permet de gérer les erreurs
        errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        };

        //Contient les différentes requetes
        requestQueue = Volley.newRequestQueue(this);

        progressBar.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.INVISIBLE);
        //On récupère tous les batiments,pièces et lumières
        getBuildings();
        getRooms();
        getLights();


        mAdapter = new HistoryAdapter(new ArrayList<String>());

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId); //On initialise le client pour se connecter sur le cloud via mqtt
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    addToHistory("Reconnected to : " + serverURI);
                    // On notifie quu'on se reconnecte
                    subscribeToTopic();
                } else {
                    addToHistory("Connected to: " + serverURI);
                    //On notifie qu'on se connecte
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                addToHistory("The Connection was lost."); //On notifie qu'on a été déconnecté
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                addToHistory("Incoming message: " + new String(message.getPayload())); //On affiche le message reçu
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        //On définit les options de connection
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName("zvafqgcd");
        mqttConnectOptions.setPassword("HgxcwISOxy_z".toCharArray());

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic(); //Si on est connecté on subscribe au Topic
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

    //Fonction permettant de set SpinnerBuilding
    private void setSpinnerBuilding() {
        List<String> buildings_names = new ArrayList<>();
        for (Building building : buildings) {//On parcout tous les batiments
            buildings_names.add(building.getName()); //On récupère le nom des batiments
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, buildings_names); //On remplit le spinner avec les noms
        spinnerBuilding.setAdapter(arrayAdapter);
    }

    //Fonction permettant de set SpinnerRoom
    private void setSpinnerRoom() {
        List<String> ids = new ArrayList<>();
        for (Room room : roomsSelected) { //On parcourt les pièces sélectionnées
            ids.add(room.getName()); //On récupère le nom des pièces
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,R.layout.spinner_item, ids);// On remplit le spinner avec les noms
        spinnerRoom.setAdapter(arrayAdapter);

    }

    //Fonction permettant de set SpinnerLight
    private void setSpinnerLight() {
        List<Long> lights_id = new ArrayList<>();
        for (Light light : lightsSelected) {// On parcourt les lumières sélectionnées
            lights_id.add(light.getId());// On récupère l'id des lumières
        }
        ArrayAdapter<Long> arrayAdapter = new ArrayAdapter<>(this,R.layout.spinner_item, lights_id);//On remplit le spinner avec les id
        spinnerLight.setAdapter(arrayAdapter);

    }

    //Fonction permettant de set SpinnerColor
    private void setSpinnerColor() {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this,R.layout.spinner_item, MainActivity.this.colors);//On remplit le spinner avec les couleurs
        spinnerColor.setAdapter(arrayAdapter);
    }

    //Fonction permettant de changer l'imageButtonLight en fonction du status de la lampe
    public void set() {
        if (light.getStatus().equals(Status.ON)) { //Cas si le status est On
            imageButtonLight.setImageResource(R.drawable.ic_bulb_on);
        } else { //Cas si le status est Off
            imageButtonLight.setImageResource(R.drawable.ic_bulb_off);
        }
        progressBar.setVisibility(View.INVISIBLE);
        scrollView.setVisibility(View.VISIBLE);
    }

    //Fonction permettant de changer le status des lampes en local et sur la base de donnée
    public void switchLightOnClick(Light light) {
        //On change en local
        if(light.getStatus().equals(Status.ON)) {
            light.setStatus(Status.OFF);
        } else {
            light.setStatus(Status.ON);
        }
        //On effectue la requete sur la base de donnée
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, urlApiLights +"/" + light.getId() + "/switch", null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String on;
                        try {
                            on = response.getString("status");
                            if(on.equalsIgnoreCase("ON")) {
                                MainActivity.this.light.setStatus(Status.ON); //On change le statut à On
                            } else {
                                MainActivity.this.light.setStatus(Status.OFF);//On change le statut à Off
                            }
                            MainActivity.this.set();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, errorListener);
        requestQueue.add(jsonObjectRequest);//On ajoute dans la queue
        progressBar.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.INVISIBLE);
    }

    public void switchLight(Light light) {
        //On change en local
        if (light.getStatus().equals(Status.ON)) {
            light.setStatus(Status.OFF);
        } else {
            light.setStatus(Status.ON);
        }
        set();
    }


    //Fonction permettant de changer la couleur des lampes en local et en base de données
    void switchColorOnCLick(Light light, String color) {
        light.setColor(color);//On change la couleur en local
        //On effectue la requête en base de données
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, urlApiLights +"/" +light.getId().toString() + "/changeColor/" + light.getColor(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String on;
                        try {
                            on = response.getString("color");
                            MainActivity.this.color = on; //On change la couleur de la lampe
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, errorListener);
        requestQueue.add(jsonObjectRequest);//On ajoute dans la queue
        MainActivity.this.color=MainActivity.this.light.getColor();
    }

    void switchColor(Light light, String color) {
        light.setColor(color);//On change la couleur en local
        MainActivity.this.color=MainActivity.this.light.getColor();
        int index = colors.indexOf(color);
        spinnerColor.setSelection(index,true);

    }
    public void getBuildings() {//Fonction effectuant la requete GET pour récupérer tous les batiments, on stock dans responseBuildings
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, urlApiBuildings, null, responseBuildings, errorListener);
        requestQueue.add(jsonArrayRequest);//On ajoute dans la queue
    }

    public void getRooms() {//Fonction effectuant la requete GET pour récupérer toutes les pièces, on stock dans responseRooms
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, urlApiRooms, null, responseRoom,errorListener);
        requestQueue.add(jsonArrayRequest);//On ajoute dans la queue
    }

    public void getLights() {//Fonction effectuant la requete GET pour récupérer toutes les lampes, on stock dans responseLights
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, urlApiLights, null, responseLight, errorListener);
        requestQueue.add(jsonArrayRequest);//On ajoute dans la queue
    }


    public void refresh(View view) {//On rafraichit en récupérant les batiments
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

    //Fonction permettant de subscribe à un topic en mqtt
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
                public void messageArrived(String topic, MqttMessage message) { //Fonction qui se lance quand le message arriver Il sera de la forme Id Order Valeur
                    System.out.println(message);
                    String[] messagesepareted = message.toString().split(" ");//On parse le string
                    Light light = MainActivity.this.lights.get(Integer.parseInt(messagesepareted[0])-1);//On récupère la lampe correspondant à l'id
                    if (messagesepareted[1].contains("switch")) { //Si l'ordre est Switch
                        if (!(messagesepareted[2].contains(light.getStatus().toString()))) { //On vérifie qu'on a un status et une valeur différents
                            switchLight(light); //On appelle switchlight
                        }
                    }
                    else if (messagesepareted[1].contains("changeColor")) {//Si l'ordre est ChangeColor
                        if (!(messagesepareted[2].contains(light.getColor()))) {//On vérifie que les couleurs sont différentes
                            switchColor(light,messagesepareted[2]);//On appelle switchcolor
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
