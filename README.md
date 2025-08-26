# Robot Controller Application - Quarkus/Java

This is a Quarkus-based robot controller application converted from the original Python/Flask implementation.

## Features

- RESTful API endpoints for robot control
- Real-time camera stream integration
- Object detection using ML inference services
- Robot movement commands (forward, backward, turn left/right)
- Obstacle detection and avoidance
- Web-based control interface

## Prerequisites

- Java 17 or higher
- Maven 3.8.1 or higher
- Docker/Podman (for containerized deployment)

## Configuration

Configure the application by setting the following environment variables:

- `ROBOT_NAME`: Your robot/team identifier
- `ROBOT_API`: URL of the robot REST API (e.g., http://robot:5000/)
- `ROBOT_API_ENDPOINT`: IP or hostname of edge-controller endpoint
- `INFERENCING_API`: URL of the object detection inferencing service
- `INFERENCING_API_TOKEN`: Authentication token for the inferencing API

Alternatively, you can modify the `src/main/resources/application.yml` file.

## Running the Application

### Development Mode

Run the application in development mode (with live reload):

```bash
./mvnw compile quarkus:dev
```

The application will be available at http://localhost:8080

### Production Mode

Build and run the application:

```bash
./mvnw clean package
java -jar target/quarkus-app/quarkus-run.jar
```

### Native Build (Optional)

Create a native executable:

```bash
./mvnw clean package -Dnative
./target/robot-controller-1.0.0-SNAPSHOT-runner
```

## Container Build and Run

Build the container image:

```bash
podman build -t robot-controller-quarkus .
```

Run the container:

```bash
podman run -p 8080:8080 \
  -e ROBOT_NAME=your-robot-name \
  -e ROBOT_API=http://your-robot-api:5000/ \
  -e INFERENCING_API=http://your-inference-api/ \
  -e INFERENCING_API_TOKEN=your-token \
  robot-controller-quarkus
```

## API Endpoints

- `GET /` - Main control interface
- `POST /run` - Start the robot
- `POST /stop` - Stop the robot  
- `POST /status` - Get robot status
- `GET /get_stream` - Get camera stream data

## Robot Programming

Implement your robot logic in the `startRobot()` method in `RobotControllerResource.java`. The default implementation includes:

- Obstacle detection and avoidance
- Object detection (looking for hats/fedoras)
- Basic search patterns

## Dependencies

Key Java libraries used:

- **Quarkus**: Supersonic Subatomic Java framework
- **RESTEasy Reactive**: Reactive REST framework
- **Jackson**: JSON processing
- **OpenCV (JavaCV)**: Image processing
- **Mutiny**: Reactive programming

## Conversion Notes

This application was converted from Python/Flask with the following mappings:

- Flask → Quarkus REST
- requests → REST Client
- OpenCV-Python → JavaCV/OpenCV  
- threading → Java concurrent utilities
- Jinja2 templates → Qute templates

## Performance

Quarkus provides:
- Fast startup time (~0.1s vs ~3s for traditional Java)
- Low memory footprint
- Native compilation support
- Live reload in development mode

## Troubleshooting

1. **Camera stream issues**: Ensure the static directory exists and has write permissions
2. **Robot API connection**: Verify ROBOT_API URL and network connectivity
3. **Inference API errors**: Check INFERENCING_API_TOKEN and endpoint availability

## Development

To add new features:

1. Add REST endpoints in `RobotControllerResource.java`
2. Implement business logic in service classes
3. Update configuration in `application.yml`
4. Add templates in `src/main/resources/templates/`
