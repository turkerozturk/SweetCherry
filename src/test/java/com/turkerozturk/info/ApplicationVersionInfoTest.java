package com.turkerozturk.info;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.info.BuildProperties;
import java.util.Properties;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class ApplicationVersionInfoTest {
    private final ApplicationContextRunner runner=new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(InfoContributorAutoConfiguration.class))
            .withUserConfiguration(ApplicationVersionInfo.class)
            .withPropertyValues("management.info.env.enabled=true","info.app.version=@project.version@","info.app.name=SweetCherry");
    @Test void artifactVersionOverridesUnfilteredExternalYamlInActualContributorOrder() {
        var properties=new Properties();properties.setProperty("version","1.1.0");
        runner.withBean(BuildProperties.class,()->new BuildProperties(properties)).run(context->{
            var builder=new Info.Builder();context.getBeanProvider(InfoContributor.class).orderedStream().forEach(c->c.contribute(builder));
            assertThat(((Map<?,?>)builder.build().getDetails().get("app")).get("version")).isEqualTo("1.1.0");
        });
    }
    @Test void sourceIdeRunHasAnExplicitDevelopmentVersionInsteadOfAToken() {
        runner.run(context->{
            var builder=new Info.Builder();context.getBeanProvider(InfoContributor.class).orderedStream().forEach(c->c.contribute(builder));
            assertThat(((Map<?,?>)builder.build().getDetails().get("app")).get("version")).isEqualTo("development");
        });
    }
}
