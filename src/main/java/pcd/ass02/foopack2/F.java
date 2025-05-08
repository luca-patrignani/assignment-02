package pcd.ass02.foopack2;

public class F {
    G g = new G();

    G.InnerG innerG = new G().new InnerG();
    void m() {
        innerG.innerMethod();
    }
}
