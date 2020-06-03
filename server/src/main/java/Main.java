import static spark.Spark.get;

public class Main {

    public static void main(String[] args) {
        get("/hello", (request, response) -> {
            System.out.print(request.userAgent());
            System.out.println(" said Hello!");
            return "Hello World";
        });
    }
}