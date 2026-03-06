package ua.dymohlo.sportPredictions.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Sport Predictions API",
                version = "1.0",
                description = """
                        REST API for the Sport Predictions application.

                        **Authentication**
                        This API uses HttpOnly JWT cookies for authentication.
                        To authenticate:
                        1. Call `POST /api/v0/auth/login` with your credentials.
                        2. The server sets a `jwt` HttpOnly cookie automatically.
                        3. All subsequent requests from the same browser session will include the cookie.

                        When using Swagger UI "Try it out": login via the `/auth/login` endpoint first,
                        then all protected endpoints will work automatically in the same session.

                        **Public endpoints** (no authentication required):
                        - `POST /api/v0/auth/register`
                        - `POST /api/v0/auth/login`
                        - `GET /api/v0/scheduler/status`
                        """
        ),
        servers = @Server(url = "/", description = "Current server")
)
@SecurityScheme(
        name = "cookieAuth",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.COOKIE,
        paramName = "jwt",
        description = "JWT token stored in HttpOnly cookie named 'jwt'. " +
                "Call POST /api/v0/auth/login first — the browser will store the cookie automatically."
)
public class OpenApiConfig {
}
