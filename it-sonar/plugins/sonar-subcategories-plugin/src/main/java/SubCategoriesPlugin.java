import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.Arrays;
import java.util.List;

public class SubCategoriesPlugin extends SonarPlugin {
  public List getExtensions() {
    return Arrays.asList(
        PropertyDefinition.builder("prop1")
            .index(2)
            .category("Category 1")
            .subCategory("Sub category 1")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder("prop2")
            .index(1)
            // SONAR-4501 category are case insensitive
            .category("category 1")
            .subCategory("Sub category 1")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder("prop3")
            .category("Category 1")
            .subCategory("Sub category 2")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder("prop5")
            .category("Category 1")
            // SONAR-4501 subcategory are case insensitive
            .subCategory("sub category 2")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder("prop4")
            .category("Category 1")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder("prop2_1")
            .category("Category 2")
            .subCategory("Sub category 1 of 2")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build(),
        PropertyDefinition.builder("prop2_2")
            .category("Category 2")
            .subCategory("Sub category 2 of 2")
            .description("Foo")
            .onQualifiers(Qualifiers.PROJECT)
            .build());
  }
}
