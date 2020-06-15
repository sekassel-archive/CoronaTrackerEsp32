package de.uniks;

import java.util.ArrayList;
import java.util.HashMap;

import static spark.Spark.*;

public class Main {

    private static HashMap<Long, ArrayList<Integer>> infections = new HashMap<>();

    public static void main(String[] args) {
        get("/hello", (request, response) -> {
            return "Hello World";
        });

        get("/infections", ((request, response) -> {
            return infections;
        }));

        post("/infections", ((request, response) -> {
            long time = System.currentTimeMillis();
            if (!infections.containsKey(time)) {
                infections.put(time, new ArrayList<>());
            }
            infections.get(time).add(Integer.parseInt(request.body()));
            return "Success!";
        }));
    }
}