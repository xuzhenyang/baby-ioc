package co.lilpilot.babyioc;

import lombok.Getter;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Named;
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

    @Test
    public void should_throw_exception_when_constructor_circular_dependency() {
        //given
        //when
        BabyContainer babyContainer = new BabyContainer();
        //then
        assertThatThrownBy(() -> babyContainer.getInstance(A.class))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("循环依赖");
    }

    @Test
    public void should_be_fine_when_singleton_circular_dependency() {
        //TODO 如果B从构造器注入，会导致有两个不一样的A实例
        //given
        //when
        BabyContainer babyContainer = new BabyContainer();
        A a = babyContainer.getInstance(A.class);
        //then
        assertThat(a).isNotNull();
        assertThat(a.getB()).isNotNull();
        assertThat(a.getB().getA()).isNotNull();
        assertThat(a.getB().getA()).isEqualTo(a);
    }

    @Test
    public void could_inject_qualifier() {
        //given
        //when
        BabyContainer babyContainer = new BabyContainer();
        babyContainer.registerQualifiedClass(Base.class, C.class);
        A a = babyContainer.getInstance(A.class);
        //then
        assertThat(a).isNotNull();
        assertThat(a.getC()).isNotNull();
        assertThat(a.getC()).isInstanceOf(C.class);
    }

}

@Getter
@Singleton
class A {
    @Inject
    private B b;

    @Inject
    @Named("c")
    private Base c;

}

@Getter
@Singleton
class B {

    @Inject
    private A a;

}

interface Base {

}

@Singleton
@Named("c")
class C implements Base {

}
