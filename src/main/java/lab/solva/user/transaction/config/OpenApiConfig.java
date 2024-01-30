package lab.solva.user.transaction.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Transaction API")
                        .description("User Transaction Information")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BRuslanB")
                                .email("kz.bars.prod@gmail.com")
                        )
                );
    }
}