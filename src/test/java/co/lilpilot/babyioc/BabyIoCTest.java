package co.lilpilot.babyioc;

import lombok.Getter;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
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

    @Test
    public void could_inject_from_field() {
        //given
        //when
        BabyContainer babyContainer = new BabyContainer();
        A a = babyContainer.getInstance(A.class);
        //then
        assertThat(a).isNotNull();
        assertThat(a.getB()).isNotNull();
    }

    @Test
    public void could_inject_from_constructor() {
        //given
        //when
        BabyContainer babyContainer = new BabyContainer();
        A a = babyContainer.getInstance(A.class);
        //then
        assertThat(a).isNotNull();
        assertThat(a.getB()).isNotNull();
    }

}

@Getter
@Singleton
class A {

    private B b;

    @Inject
    public A(B b) {
        this.b = b;
    }

}

class B {

}
