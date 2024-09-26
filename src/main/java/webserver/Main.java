package webserver;

import com.fastcgi.FCGIInterface;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

class Main {
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static double r;
    private static final String RESULT_JSON = """
            {
                "answer": %b,
                "executionTime": "%s",
                "serverTime": "%s"
            }
            """;
    private static final String HTTP_RESPONSE = """
            HTTP/1.1 200 OK
            Content-Type: application/json
            Content-Length: %d
                    
            %s
            """;
    private static final String HTTP_ERROR = """
            HTTP/1.1 400 Bad Request
            Content-Type: application/json
            Content-Length: %d
                    
            %s
            """;
    private static final String ERROR_JSON = """
            {
                "error": "Хватит ломать мне лабу((",
                "reason": "%s"
            }
            """;

    public static void main(String[] args) throws IOException {
        var fcgiInterface = new FCGIInterface();
        while (fcgiInterface.FCGIaccept() >= 0) {
            long startTime = System.nanoTime();
            try {
                var queryParams = System.getProperties().getProperty("QUERY_STRING");

                Map<String, String> params = new HashMap<>();
                if (queryParams != null && !queryParams.isEmpty()) {
                    String[] pairs = queryParams.split("&");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=");
                        if (keyValue.length == 2) {
                            params.put(keyValue[0], keyValue[1]);
                        }
                    }
                }

                if (!params.containsKey("x") || !params.containsKey("y") || !params.containsKey("r")) {
                    throw new IllegalArgumentException("Miss req pls (x, y, r).");
                }

                double x = parseAndValidateDouble(params.get("x"), "x");
                double y = parseAndValidateDouble(params.get("y"), "y");
                r = parseAndValidateDouble(params.get("r"), "r");

                if (x < -5 || x > 3) {
                    throw new IllegalArgumentException("x only -5 and 3.");
                }
                if (y < -5 || y > 5) {
                    throw new IllegalArgumentException("y only -5 and 5.");
                }
                if (r <= 0 || r > 5) {
                    throw new IllegalArgumentException("r only 0 and 5.");
                }

                double[][] polygon1 = {
                        {0.35 * r, 0.75 * r},
                        {-0.6 * r, -0.3 * r},
                        {-0.25 * r, -0.3 * r},
                        {0.6 * r, 0.75 * r}
                };

                double[][] polygon2 = {
                        {-0.2 * r, 0.75 * r},
                        {-0.6 * r, 0.35 * r},
                        {-0.35 * r, 0.35 * r},
                        {0.05 * r, 0.75 * r}
                };

                double[][] polygon3 = {
                        {0.35 * r, 0.1 * r},
                        {0, -0.3 * r},
                        {0.25, -0.3 * r},
                        {0.6 * r, 0.1 * r}
                };

                boolean insideRectangle1 = isInsideRectangle(x, y, -0.6 * r, r, 1.2 * r, 0.25 * r);
                boolean insideRectangle2 = isInsideRectangle(x, y, -0.6 * r, 0.35 * r, 1.2 * r, 0.25 * r);
                boolean insideRectangle3 = isInsideRectangle(x, y, -0.6 * r, -0.3 * r, 1.2 * r, 0.25 * r);
                boolean insidePolygon1 = isInsidePolygon(x, y, polygon1);
                boolean insidePolygon2 = isInsidePolygon(x, y, polygon2);
                boolean insidePolygon3 = isInsidePolygon(x, y, polygon3);

                var json = String.format(RESULT_JSON, insideRectangle1 || insideRectangle2 || insideRectangle3 ||
                        insidePolygon1 || insidePolygon2 || insidePolygon3, System.nanoTime() - startTime, LocalDateTime.now().format(formatter));
                var responseBody = json.trim();
                var response = String.format(HTTP_RESPONSE, responseBody.getBytes(StandardCharsets.UTF_8).length, responseBody);
                try {
                    FCGIInterface.request.outStream.write(response.getBytes(StandardCharsets.UTF_8));
                    FCGIInterface.request.outStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (Exception ex) {
                var json = String.format(ERROR_JSON, ex.getMessage());
                var responseBody = json.trim();
                var response = String.format(HTTP_ERROR, responseBody.getBytes(StandardCharsets.UTF_8).length, responseBody);
                FCGIInterface.request.outStream.write(response.getBytes(StandardCharsets.UTF_8));
                FCGIInterface.request.outStream.flush();
            }
        }
    }

    private static double parseAndValidateDouble(String value, String paramName) throws IllegalArgumentException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("its invalid '%s'. Give me valid pls.", paramName));
        }
    }

    private static boolean isInsideRectangle(double px, double py, double rectX, double rectY, double width, double height) {
        return (px >= rectX && px <= rectX + width) && (py <= rectY && py >= rectY - height);
    }

    private static boolean isInsidePolygon(double px, double py, double[][] polygon) {
        int n = polygon.length;
        boolean inside = false;

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon[i][0], yi = polygon[i][1];
            double xj = polygon[j][0], yj = polygon[j][1];

            boolean intersect = ((yi > py) != (yj > py)) && (px < (xj - xi) * (py - yi) / (yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }

        return inside;
    }
}
