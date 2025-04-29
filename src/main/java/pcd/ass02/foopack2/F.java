package pcd.ass02.foopack2;

public class F {
    G g = new G();

    G.innerG innerG = new G().new innerG();
    void m() {
        innerG.innerMethod();
    }
}
