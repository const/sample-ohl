package sample.cofinguration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.relational.RelationalManagedTypes;
import org.springframework.data.relational.core.mapping.DefaultNamingStrategy;
import org.springframework.data.relational.core.mapping.NamingStrategy;

import java.util.Optional;

@Configuration
@EnableConfigurationProperties(TestProperties.class)
public class SpringDataJdbcAdditionalConfiguration {

    @Bean
    public JdbcMappingContext jdbcMappingContext(Optional<NamingStrategy> namingStrategy,
                                                 JdbcCustomConversions customConversions,
                                                 RelationalManagedTypes jdbcManagedTypes,
                                                 TestProperties testProperties) {
        JdbcMappingContext mappingContext = new JdbcMappingContext(namingStrategy.orElse(
                DefaultNamingStrategy.INSTANCE));
        mappingContext.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());
        mappingContext.setManagedTypes(jdbcManagedTypes);
        mappingContext.setSingleQueryLoadingEnabled(
                testProperties.getSpringDataJdbcProperties()
                        .isSingleQueryLoadingEnabled());
        return mappingContext;
    }
}
