package co.lilpilot.babyioc;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

public class BabyIoCTest {

    @Test
    public void should_create_instance() {
        // when
        BabyContainer babyContainer = new BabyContainer();
        A instance = babyContainer.getInstance(A.class);
        // then
        assertThat(instance).isNotNull();
    }

}

class A {

}
