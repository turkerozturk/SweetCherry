package com.turkerozturk.icons;
import com.turkerozturk.helpers.NodeIcon;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.context.support.StaticMessageSource;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class IconCatalogueTest {
    static ResourceBundleMessageSource messages(){var m=new ResourceBundleMessageSource();m.setBasename("messages");m.setDefaultEncoding("UTF-8");return m;}
    final IconController controller=new IconController(messages());
    @Test void allIconsHaveEditableCategoriesAndAllBundledCategoriesHaveBothTranslations(){
        var messages=messages();
        assertThat(Arrays.stream(NodeIcon.values())).allMatch(i->i.getCategory()!=null&&!i.getCategory().isBlank());
        assertThat(NodeIcon.CT_CS.getCategory()).isEqualTo("flags");assertThat(NodeIcon.CT_TA.getCategory()).isEqualTo("flags");
        assertThat(NodeIcon.CT_CSHARP.getCategory()).isEqualTo("programming");assertThat(NodeIcon.CT_PYTHON.getCategory()).isEqualTo("programming");
        assertThat(NodeIcon.CT_BAT.getCategory()).isEqualTo("animals");assertThat(NodeIcon.CT_TUX.getCategory()).isEqualTo("technology");
        for(var locale:List.of(Locale.ENGLISH,Locale.forLanguageTag("tr")))for(var icon:NodeIcon.values())assertThat(messages.getMessage("icons.category."+icon.getCategory(),null,locale)).isNotBlank();
    }
    @Test void originalIndicesStayStableThroughSortingAndFiltering(){
        var ascending=controller.listing(new IconController.Options("index","asc","",""),Locale.ENGLISH).rows();
        var descending=controller.listing(new IconController.Options("index","desc","",""),Locale.ENGLISH).rows();
        assertThat(ascending).hasSize(NodeIcon.values().length);assertThat(ascending.get(0).icon()).isSameAs(NodeIcon.OTHER);
        assertThat(ascending.get(0).index()).isEqualTo(1);assertThat(descending.get(0).icon()).isSameAs(NodeIcon.CT_TA);
        assertThat(descending.get(0).index()).isEqualTo(264);
        var filtered=controller.listing(new IconController.Options("name","desc","cat","animals"),Locale.ENGLISH).rows();
        assertThat(filtered).hasSize(1);assertThat(filtered.get(0).icon()).isSameAs(NodeIcon.CT_CAT);assertThat(filtered.get(0).index()).isEqualTo(NodeIcon.CT_CAT.ordinal()+1);
    }
    @Test void localizedCategoryAndNameSortsSupportBothDirections(){
        var asc=controller.listing(new IconController.Options("category","asc","",""),Locale.ENGLISH).rows();
        var desc=controller.listing(new IconController.Options("category","desc","",""),Locale.ENGLISH).rows();
        assertThat(asc.get(0).categoryLabel()).isEqualTo("Actions");assertThat(desc.get(0).categoryLabel()).isEqualTo("Weather");
        assertThat(asc.stream().filter(r->r.icon().getCategory().equals("animals")).limit(4).map(r->r.icon())).containsExactly(NodeIcon.CT_BAT,NodeIcon.CT_BEAR,NodeIcon.CT_BUG,NodeIcon.CT_BUTTERFLY);
        var names=controller.listing(new IconController.Options("name","asc","","animals"),Locale.ENGLISH).rows();
        var namesDesc=controller.listing(new IconController.Options("name","desc","","animals"),Locale.ENGLISH).rows();
        assertThat(names.get(0).icon()).isSameAs(NodeIcon.CT_BAT);assertThat(namesDesc.get(0).icon()).isSameAs(NodeIcon.CT_ZEBRA);
        var tr=controller.listing(new IconController.Options("category","asc","","flags"),Locale.forLanguageTag("tr"));
        assertThat(tr.rows()).isNotEmpty().allMatch(row->row.categoryLabel().equals("Bayraklar"));
    }
    @Test void normalizedSearchFindsTurkishLabelsAndCanonicalNamesAndUnknownTranslationsFallBack(){
        var tr=controller.listing(new IconController.Options("category","asc","ISLEMLER",""),Locale.forLanguageTag("tr"));
        assertThat(tr.rows()).isNotEmpty().allMatch(r->r.icon().getCategory().equals("actions"));
        assertThat(controller.listing(new IconController.Options("category","asc","ct_spring-boot",""),Locale.ENGLISH).rows()).extracting(IconController.Row::icon).containsExactly(NodeIcon.CT_SPRING_BOOT);
        var fallback=new IconController(new StaticMessageSource()).listing(new IconController.Options("category","asc","","flags"),Locale.ENGLISH);
        assertThat(fallback.rows()).isNotEmpty().allMatch(r->r.categoryLabel().equals("flags"));
        assertThatThrownBy(()->new IconController.Options("invalid","asc","","")).hasMessageContaining("400");
        assertThatThrownBy(()->controller.listing(new IconController.Options("name","asc","","not-a-category"),Locale.ENGLISH)).hasMessageContaining("400");
    }
}
