package com.redhat.robotics;

import com.redhat.robotics.client.RobotApiClient;
import com.redhat.robotics.config.RobotConfig;
import com.redhat.robotics.model.StreamResponse;
import com.redhat.robotics.service.RobotUtilsService;
import io.quarkus.logging.Log;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
@Path("/")
public class RobotControllerResource {

    @Inject
    Template index;

    @Inject
    RobotUtilsService robotUtilsService;

    @Inject
    @RestClient
    RobotApiClient robotApiClient;

    @Inject
    RobotConfig robotConfig;

    // Thread-safe flags for robot control
    private final AtomicBoolean threadEvent = new AtomicBoolean(false);
    private final AtomicBoolean hatFoundAndIntercepted = new AtomicBoolean(false);
    private final AtomicReference<Thread> robotThread = new AtomicReference<>();

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String index() {
        try {
            // Read the actual index.html template
            String htmlContent = java.nio.file.Files.readString(
                java.nio.file.Paths.get("src/main/resources/templates/index.html")
            );
            return htmlContent;
        } catch (Exception e) {
            Log.errorf("Error loading template: %s", e.getMessage());
            // Fallback to a working robot controller interface
            return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Robot Control Interface</title>
                    <link href="https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700&display=swap" rel="stylesheet">
                    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
                    <script src="https://cdn.tailwindcss.com"></script>
                    <style>
                        body { font-family: 'Orbitron', sans-serif; background-color: #0a0f18; color: #e0e0e0; }
                        .glow-btn { background-color: transparent; border: 1px solid #00aaff; color: #00eaff; 
                                   text-transform: uppercase; letter-spacing: 2px; transition: all 0.3s ease; 
                                   box-shadow: 0 0 5px rgba(0, 170, 255, 0.5); }
                        .glow-btn:hover { background-color: rgba(0, 170, 255, 0.2); color: #ffffff; 
                                         box-shadow: 0 0 15px rgba(0, 170, 255, 0.8); }
                    </style>
                </head>
                <body class="flex items-center justify-center min-h-screen p-4">
                    <main class="w-full max-w-4xl mx-auto p-6 rounded-lg" style="background-color: rgba(13, 22, 41, 0.8); border: 1px solid #00aaff;">
                        <header class="text-center mb-8">
                            <h1 class="text-4xl font-bold text-cyan-300 mb-2">Robot Controller</h1>
                            <p class="text-cyan-400">Quarkus Java Application</p>
                        </header>
                        
                        <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                            <button class="glow-btn py-3 px-4 rounded-md" onclick="sendCommand('status', this)">
                                <i class="bi bi-hdd-stack"></i> Check Status
                            </button>
                            <button class="glow-btn py-3 px-4 rounded-md" onclick="sendCommand('run', this)">
                                <i class="bi bi-play-fill"></i> Start Robot
                            </button>
                            <button class="glow-btn py-3 px-4 rounded-md" onclick="sendCommand('stop', this)">
                                <i class="bi bi-stop-fill"></i> Stop Robot
                            </button>
                        </div>
                        
                        <div id="output" class="mt-6 p-4 rounded-md text-sm font-mono" 
                             style="background-color: rgba(0, 0, 0, 0.3); border: 1px dashed rgba(0, 170, 255, 0.3); color: #00eaff;">
                            Ready for commands...
                        </div>
                    </main>
                    
                    <script>
                        async function sendCommand(endpoint, button) {
                            const output = document.getElementById('output');
                            const originalText = button.innerHTML;
                            button.innerHTML = '<i class="bi bi-arrow-repeat"></i> Sending...';
                            
                            try {
                                const response = await fetch('/' + endpoint, { method: 'POST' });
                                const result = await response.text();
                                output.textContent = endpoint.toUpperCase() + ' Response: ' + result;
                            } catch (error) {
                                output.textContent = 'Error: ' + error.message;
                            } finally {
                                button.innerHTML = originalText;
                            }
                        }
                    </script>
                </body>
                </html>
                """;
        }
    }

    @POST
    @Path("/run")
    @Produces(MediaType.TEXT_PLAIN)
    public String run() {
        try {
            robotUtilsService.logWithTimestamp("/run endpoint called.");
            threadEvent.set(true);
            hatFoundAndIntercepted.set(false);
            
            // Stop any existing robot thread
            Thread existingThread = robotThread.get();
            if (existingThread != null && existingThread.isAlive()) {
                existingThread.interrupt();
            }
            
            robotUtilsService.logWithTimestamp("Creating and starting the startRobot thread.");
            Thread thread = new Thread(this::startRobot);
            robotThread.set(thread);
            thread.start();
            
            robotUtilsService.logWithTimestamp("/run endpoint finished and returned 'Robot started'.");
            return "Robot started";
        } catch (Exception error) {
            Log.errorf("Error in run endpoint: %s", error.getMessage());
            return "Error: " + error.getMessage();
        }
    }

    @POST
    @Path("/stop")
    @Produces(MediaType.TEXT_PLAIN)
    public String stop() {
        try {
            robotUtilsService.logWithTimestamp("/stop endpoint called.");
            threadEvent.set(false);
            
            // Interrupt the robot thread
            Thread thread = robotThread.get();
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
            
            return "Robot stopped";
        } catch (Exception error) {
            Log.errorf("Error in stop endpoint: %s", error.getMessage());
            return "Error: " + error.getMessage();
        }
    }

    @POST
    @Path("/status")
    @Produces(MediaType.TEXT_PLAIN)
    public String status() {
        try {
            return robotApiClient.getRemoteStatus(robotConfig.name());
        } catch (Exception error) {
            Log.errorf("Error getting status: %s", error.getMessage());
            return "Error: " + error.getMessage();
        }
    }

    @GET
    @Path("/get_stream")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStream() {
        String staticImagePath = "src/main/resources/META-INF/resources/static/current_view.jpg";
        java.nio.file.Path imagePath = Paths.get(staticImagePath);
        
        if (!Files.exists(imagePath)) {
            StreamResponse errorResponse = new StreamResponse("Image file not found: " + staticImagePath, true);
            return Response.status(404).entity(errorResponse).build();
        }

        try {
            byte[] imageBytes = Files.readAllBytes(imagePath);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            StreamResponse response = new StreamResponse(base64Image);
            return Response.ok(response).build();
        } catch (Exception e) {
            Log.errorf("Failed to read static image %s: %s", staticImagePath, e.getMessage());
            StreamResponse errorResponse = new StreamResponse("Failed to read image file: " + e.getMessage(), true);
            return Response.status(500).entity(errorResponse).build();
        }
    }

    @GET
    @Path("/{pageName}")
    @Produces(MediaType.TEXT_HTML)
    public Response servePage(@PathParam("pageName") String pageName) {
        try {
            // Try to load the template
            InputStream templateStream = getClass().getClassLoader()
                .getResourceAsStream("templates/" + pageName + ".html");
            
            if (templateStream == null) {
                return Response.status(404)
                    .entity("<h1>Page not found</h1><p>The requested page does not exist.</p>")
                    .build();
            }
            
            String content = new String(templateStream.readAllBytes());
            return Response.ok(content).build();
        } catch (Exception e) {
            Log.errorf("Error serving page %s: %s", pageName, e.getMessage());
            return Response.status(500)
                .entity("<h1>Error</h1><p>An error occurred while loading the page.</p>")
                .build();
        }
    }

    @GET
    @Path("/templates/{filename}")
    public Response serveTemplateAsset(@PathParam("filename") String filename) {
        try {
            InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream("META-INF/resources/templates/" + filename);
            
            if (resourceStream == null) {
                return Response.status(404).build();
            }
            
            String contentType = getContentType(filename);
            byte[] content = resourceStream.readAllBytes();
            
            return Response.ok(content)
                .type(contentType)
                .build();
        } catch (Exception e) {
            Log.errorf("Error serving template asset %s: %s", filename, e.getMessage());
            return Response.status(500).build();
        }
    }

    private String getContentType(String filename) {
        if (filename.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (filename.endsWith(".css")) {
            return "text/css";
        } else if (filename.endsWith(".js")) {
            return "application/javascript";
        } else if (filename.endsWith(".html")) {
            return "text/html";
        }
        return "application/octet-stream";
    }

    /**
     * Main robot logic - implement your robot behavior here
     */
    private void startRobot() {
        try {
            robotUtilsService.logWithTimestamp("Robot started - implement your robot logic here");
            
            // Example robot logic - you can modify this
            int turnCounter = 0;
            AtomicReference<Boolean> hatFoundRef = new AtomicReference<>(false);
            
            while (threadEvent.get() && !Thread.currentThread().isInterrupted() && !hatFoundRef.get()) {
                try {
                    // Check for obstacles
                    if (robotUtilsService.bypassObstacle()) {
                        continue; // Skip this iteration if obstacle was bypassed
                    }
                    
                    // Search for hat
                    turnCounter = robotUtilsService.searchForHatStep(turnCounter, hatFoundRef);
                    
                    // Small delay to prevent overwhelming the system
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    robotUtilsService.logWithTimestamp("Error in robot loop: " + e.getMessage());
                    Thread.sleep(1000); // Wait before retrying
                }
            }
            
            robotUtilsService.logWithTimestamp("Robot execution completed");
        } catch (Exception e) {
            robotUtilsService.logWithTimestamp("Robot thread error: " + e.getMessage());
        }
    }
}
