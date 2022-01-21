import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;


class HttpTest{

    static ExecutorService executor = Executors.newFixedThreadPool(6, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable){
            Thread thread = new Thread(runnable);
            System.out.println("New Thread created"+(thread.isDaemon()? ":daemon": ""+"\nThread Group: "+thread.getThreadGroup()));
            return thread;
        }
    });

    public static void main(String[] args) throws IOException, InterruptedException{

        connectAkamaiHttp1Client();

    }

    private static void connectAkamaiHttp1Client(){

        System.out.println("Running HTTP/1.1 example...");

        try{

            HttpClient httpClient = HttpClient.newBuilder()
                                              .version(HttpClient.Version.HTTP_1_1)
                                              .proxy(ProxySelector.getDefault())
                                              .build();

            long startT = System.currentTimeMillis();
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create("https://http2.akamai.com/demo/h2_demo_frame.html"))
                                             .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Status code: "+ response.statusCode());
            System.out.println("Response Headers: "+response.headers());
            String responseBody = response.body();
            System.out.println("Body: "+responseBody);

            List<Future<?>> future = new ArrayList<>(); 

            responseBody.lines()
                        .filter(line -> line.trim().startsWith("<img height"))
                        .map(line -> line.substring(line.indexOf("src=''")+5, line.indexOf("/>")))
                        .forEach(image -> {
                            Future<?> imageFuture = executor.submit(()->{
                                HttpRequest imageRequest = HttpRequest.newBuilder()
                                                                      .uri(URI.create("https://http2.akamai.com"+image))
                                                                      .build();
                                try {
                                    HttpResponse<String> imageResponse = httpClient.send(imageRequest, HttpResponse.BodyHandlers.ofString());
                                    System.out.println("Image Loaded: "+image+"\nStatus Code: "+imageResponse.statusCode());
                                } catch (IOException | InterruptedException e) {
                                    e.printStackTrace();
                                }
                            });
                            future.add(imageFuture);
                            System.out.println("Submitted images: "+image);
                        });
            future.forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });

            long finalT = System.currentTimeMillis();
            System.out.println("Time of execution: "+(finalT-startT));

        } catch(IOException | InterruptedException e){
            e.printStackTrace();
        }finally{
            executor.shutdown();
        }

    }

    private static void printConnectJavaOracle() throws IOException, InterruptedException{

        HttpRequest request = HttpRequest.newBuilder()
                                         .GET()
                                         .uri(URI.create("https://docs.oracle.com/javase/10/language/"))
                                         .build();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status Code: "+response.body());
        System.out.println("Header Code: "+response.headers());
        System.out.println("Body Code: "+response.body());

    }

}
