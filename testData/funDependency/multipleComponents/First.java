package funDependency.multipleComponents;

/**
 * @author abreslav
 */
public class First {
    void a9(Object o) { a6(o); }
    void a2(Object o) { a3(o); a4(o);}
    void a4(Object o) { a3(o); a5(o);}
    void a5(Object o) { a6(o); }
    void a3(Object o) { a4(o); }
    void a1(Object o) { a2(o); }
    void a8(Object o) {}
    void a7(Object o) { a9(o); }
    void a6(Object o) { a7(o); a9(o); }
}
