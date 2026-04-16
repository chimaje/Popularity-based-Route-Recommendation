package proj.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import proj.util.RouteGenerate;
import proj.util.RouteResult;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class RouteController {

    @Autowired
    private RouteGenerate routeGenerate;

    @PostMapping("/generate-route")
    public RouteResult generateRoute(@RequestBody GenerateRouteRequest request) {
        return routeGenerate.generateroute(request.startLat, request.startLng, request.endLat, request.endLng, request.weightType);
    }

    public static class GenerateRouteRequest {
        public double startLat;
        public double startLng;
        public double endLat;
        public double endLng;
        public String weightType;
    }
} 

