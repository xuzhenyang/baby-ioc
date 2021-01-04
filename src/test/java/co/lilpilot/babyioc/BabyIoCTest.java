package co.lilpilot.babyioc;

import org.junit.jupiter.api.Test;

import javax.inject.Singleton;

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

    @Test
    public void should_return_exist_when_singleton() {
        //given
        //when
        BabyContainer babyContainer = new BabyContainer();
        A a1 = babyContainer.getInstance(A.class);
        A a2 = babyContainer.getInstance(A.class);
        //then
        assertThat(a1).isEqualTo(a2);
    }

}

@Singleton
class A {

}
