
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.geometry.Cylinder;
import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.*;
import java.util.ArrayList;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;





public class ArticulatedArmRobot extends JFrame implements KeyListener {

    private Timer zegar = new Timer();
    private ViewingPlatform vPlatform;

    private boolean CzyNagrywa = false;
    private boolean trzyma = false;
    private boolean odtworz_ruch = false;

    // interpolatory obracające
    private RotationInterpolator RotInt_podstawa; // podstawa robota
    private RotationInterpolator RotInt_przegub; // pierwsze ramie
    private RotationInterpolator RotInt_przegub2; // drugie ramie

    // katy przesunięć robota
    private float α_podstawa = 0f; // kąt przesunięcia bazy robota
    private float α_przegub = 0f; // kat przesuniecia ramienia1
    private float α_przegub2 = 0f;

    //TransformGroupy
    private TransformGroup TG_podloze;
    private TransformGroup TG_podstawa;
    private TransformGroup TG_przegub;
    private TransformGroup TG_przegub2;
    private TransformGroup TG_ramie;
    private TransformGroup TG_ramie2;
    private TransformGroup TG_chwytak;
    private TransformGroup TG_klocek;


    //Transformy 3d
    private Transform3D T3d_podloze;
    private Transform3D T3d_podstawa;
    private Transform3D T3d_przegub;
    private Transform3D T3d_przegub_rot;
    private Transform3D T3d_przegub2;
    private Transform3D T3d_przegub2_rot;
    private Transform3D T3d_ramie;
    private Transform3D T3d_ramie2;
    private Transform3D T3d_ramie2_rot;
    private Transform3D T3d_chwytak;
    private Transform3D T3d_klocek;


    Material material = new Material(new Color3f(2.0f, 0.2f, 0.2f), new Color3f(0.0f, 1.0f, 0.0f), new Color3f(1.0f, 1.0f, 1.0f), new Color3f(1.0f, 0.0f, 0.0f), 10.0f);
    Material material2 = new Material(new Color3f(0.2f, 0.2f, 1.2f), new Color3f(0.0f, 1.0f, 0.0f), new Color3f(1.0f, 1.0f, 1.0f), new Color3f(0.0f, 0.0f, 0.0f), 10.0f);

    // wyglad obiektow
    Appearance wyglad_podloze;
    Appearance wyglad_podstawa;
    Appearance wyglad_przegub;
    Appearance wyglad_ramie;
    Appearance wyglad_ramie2;
    Appearance wyglad_przegub2;
    Appearance wyglad_chwytak;
    Appearance wyglad_tekst;
    Appearance wyglad_niebo;
    Appearance wyglad_klocek;


    // bryły
    Cylinder waist;
    Cylinder przegub;
    Cylinder przegub2;
    Cylinder chwytak;
    Box ramie;
    Box ramie2;
    Box klocek;

    // scena
    private Canvas3D canvas;
    private SimpleUniverse s_univ;
    private JPanel jPanel;
    private JButton przyciski[];
    private JTextField text;

    static final double ruch = (Math.PI)/120; //kąt obrotu
    int klawisz_ruchu;
    
    ArrayList <PozycjaRobota> nagranie = new ArrayList<PozycjaRobota>();
    int klatka = 0;
    
    //ladowanie tekstur
    TextureLoader tloader;


    public ArticulatedArmRobot() {

        super("ArticulatedArmRobot"); // nazwa u góry okienka

        //canvas i universe
        canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
        s_univ = new SimpleUniverse(canvas);
        s_univ.getViewingPlatform().setNominalViewingTransform();
        canvas.addKeyListener(this);

        utworzScene();
 
        //pozycja kamery na start
        pozycjaKamery(new Vector3f(0f, 1.5f, 12.0f));

        //zegar aby odświeżać ekran
        zegar.scheduleAtFixedRate(new Poruszanie(), 0, 10);
        new Timer().scheduleAtFixedRate(new OdegranieRuchu(), 50, 50);



    }

    public static void main(String args[]) {
                new ArticulatedArmRobot().setVisible(true);
    }



     private class ObslugaPrzycisku implements ActionListener {
        private JFrame ref_okno;

        ObslugaPrzycisku(JFrame okno){
            ref_okno = okno;
        }
        
        public void actionPerformed(ActionEvent e) {
            JButton bt = (JButton)e.getSource();
            if(bt==przyciski[0]) {
             pozycjaKamery(new Vector3f(0f, 1.5f, 12.0f));
            }
         if(bt==przyciski[1]) {
             JOptionPane.showMessageDialog(ref_okno, "strzałki (prawo, lewo)-waist\n " + "strzałki (góra, dół)- shoulder\n " + "w, s - elbow\n" + "n - nagrywanie\n" + "m - odtwarzanie\n" + "ESCAPE - zakończenie odtwarzania\n" + "SPACE - łapanie/puszczanie klocka");
            }
        }
     }

    
    
    
    public BranchGroup utworzRobota() {
        
        
        // Branchgroup
        BranchGroup scena = new BranchGroup();
        
        //tworzenie przyciskow 
        przyciski = new JButton[4];
        przyciski[0] = new JButton("Reset kamery");
        przyciski[0].addActionListener(new ObslugaPrzycisku(this));

        przyciski[1] = new JButton("Instrukcja");
        przyciski[1].addActionListener(new ObslugaPrzycisku(this));

        JPanel panelPrzyciski   = new JPanel(new FlowLayout());
        panelPrzyciski.add(przyciski[0]);
        panelPrzyciski.add(przyciski[1]);
        
        text = new JTextField("swobodne poruszanie", 12);
        
        panelPrzyciski.add(text);
        
        Container content = getContentPane();
        content.add(panelPrzyciski,BorderLayout.SOUTH);
        
        
        //TEKST
       
        
        //wyglad nieba 
        wyglad_niebo = new Appearance();
        tloader = new TextureLoader("obrazki/niebo1.jpg", null);
        ImageComponent2D image = tloader.getImage();
        Texture2D sfera = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                        image.getWidth(), image.getHeight());
        sfera.setImage(0, image);
        sfera.setBoundaryModeS(Texture.WRAP);
        sfera.setBoundaryModeT(Texture.WRAP);
        wyglad_niebo.setTexture(sfera);
        Sphere niebo = new Sphere(7f, Sphere.GENERATE_NORMALS_INWARD|Sphere.GENERATE_TEXTURE_COORDS, wyglad_niebo);
        

        //wyglad podloza
        wyglad_podloze = new Appearance();

        tloader = new TextureLoader("obrazki/1.jpg", null);
        ImageComponent2D obrazek = tloader.getImage();
        Texture2D tekstura = new Texture2D(Texture.BASE_LEVEL, Texture.RGBA,
                                        obrazek.getWidth(), obrazek.getHeight());
        tekstura.setImage(0, obrazek);
        
        wyglad_podloze.setTexture(tekstura);

        //wyglad obiektow
        wyglad_podstawa = new Appearance();
        wyglad_przegub = new Appearance();
        wyglad_przegub2 = new Appearance();
        wyglad_ramie = new Appearance();
        wyglad_ramie2 = new Appearance();
        wyglad_chwytak = new Appearance();
        wyglad_klocek = new Appearance();

        wyglad_podstawa.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
        wyglad_przegub.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
        wyglad_przegub2.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
        wyglad_ramie.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
        wyglad_ramie2.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
        wyglad_chwytak.setCapability(Appearance.ALLOW_MATERIAL_WRITE);
        wyglad_klocek.setCapability(Appearance.ALLOW_MATERIAL_WRITE);

        wyglad_podstawa.setMaterial(material);
        wyglad_przegub.setMaterial(material2);
        wyglad_przegub2.setMaterial(material2);
        wyglad_ramie.setMaterial(material);
        wyglad_ramie2.setMaterial(material);
        wyglad_chwytak.setMaterial(material2);
        wyglad_klocek.setMaterial(material2);

        // Ruch
        Alpha alpha = new Alpha(-1, 5000);
   

        //TRANSFORMGROUPY
        TG_podloze = new TransformGroup();
        TG_podstawa = new TransformGroup();
        TG_przegub = new TransformGroup();
        TG_przegub2 = new TransformGroup();
        TG_ramie = new TransformGroup();
        TG_ramie2 = new TransformGroup();
        TG_chwytak = new TransformGroup();
        TG_klocek = new TransformGroup();

        TG_podloze.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        TG_podstawa.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        TG_przegub.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        TG_przegub2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        TG_ramie.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        TG_ramie2.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        TG_chwytak.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        TG_klocek.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        
        //OGRANICZENIE
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);

        //TRANSFORMY 3D
        T3d_podloze = new Transform3D();
        T3d_podstawa = new Transform3D();
        T3d_przegub = new Transform3D();
        T3d_przegub_rot = new Transform3D();
        T3d_przegub2 = new Transform3D();
        T3d_przegub2_rot = new Transform3D();
        T3d_ramie = new Transform3D();
        T3d_ramie2 = new Transform3D();
        T3d_ramie2_rot = new Transform3D();
        T3d_chwytak = new Transform3D();
        T3d_klocek = new Transform3D();




        //podloze
        Cylinder podloze = new Cylinder(5f, 0.1f, Cylinder.GENERATE_TEXTURE_COORDS, wyglad_podloze);

        T3d_podloze.set(new Vector3f(0f, 0.0f, 0.0f));
        TG_podloze.setTransform(T3d_podloze);
        TG_podloze.addChild(podloze);
        TG_podloze.addChild(TG_podstawa);

        // obrot podstawy
        T3d_podstawa.set(new Vector3f(0.0f, 0.0f, 0.0f));
        RotInt_podstawa = new RotationInterpolator(alpha, TG_podstawa, T3d_podstawa, 0, 0);
        RotInt_podstawa.setSchedulingBounds(bounds);
        TG_podstawa.addChild(RotInt_podstawa);

        TransformGroup tg_przesuniecie_podstawa = new TransformGroup();
        Transform3D t3d_przesuniecie_podstawa = new Transform3D();
        t3d_przesuniecie_podstawa.set(new Vector3f(0f, 0.5f, 0f));
        waist = new Cylinder(0.3f, 1.0f, wyglad_podstawa);
        tg_przesuniecie_podstawa.setTransform(t3d_przesuniecie_podstawa);
        tg_przesuniecie_podstawa.addChild(waist);

        TG_podstawa.addChild(tg_przesuniecie_podstawa);

        // przegub
        T3d_przegub_rot.rotX(-Math.PI / 2);
        T3d_przegub.set(new Vector3f(0f, 1.2f, 0.0f));
        T3d_przegub.mul(T3d_przegub_rot);
        RotInt_przegub = new RotationInterpolator(alpha, TG_przegub, T3d_przegub, 0, 0);
        RotInt_przegub.setSchedulingBounds(bounds);
        TG_przegub.addChild(RotInt_przegub);

        TransformGroup tg_przegub = new TransformGroup();
        tg_przegub.setTransform(T3d_przegub);
        przegub = new Cylinder(0.4f, 0.6f, wyglad_przegub);
        tg_przegub.addChild(przegub);

        TG_przegub.addChild(tg_przegub);
        TG_podstawa.addChild(TG_przegub);

        // przegub2
        T3d_przegub2_rot.rotX(Math.PI / 2);
        T3d_przegub2.set(new Vector3f(1.2f, 0.0f, 0.0f));
        T3d_przegub2.mul(T3d_przegub2_rot);
        RotInt_przegub2 = new RotationInterpolator(alpha, TG_przegub2, T3d_przegub2, 0, 0);
        RotInt_przegub2.setSchedulingBounds(bounds);
        TG_przegub2.addChild(RotInt_przegub2);

        TransformGroup tg_przegub2 = new TransformGroup();
        przegub2 = new Cylinder(0.25f, 0.33f, wyglad_przegub2);
        tg_przegub2.setTransform(T3d_przegub2);
        tg_przegub2.addChild(przegub2);

        TG_przegub2.addChild(tg_przegub2);
        TG_ramie.addChild(TG_przegub2);

        // ramie
        ramie = new Box(1.0f, 0.12f, 0.15f, wyglad_ramie);
        T3d_ramie.set(new Vector3f(1.0f, 1.2f, 0.0f));
        TG_ramie.setTransform(T3d_ramie);
        TG_ramie.addChild(ramie);
        TG_przegub.addChild(TG_ramie);

        // ramie2
        T3d_ramie2_rot.rotZ(Math.PI / 2);
        T3d_ramie2.set(new Vector3f(1.2f, -0.4f, 0.0f));
        T3d_ramie2.mul(T3d_ramie2_rot);

        TransformGroup tg_ramie2 = new TransformGroup();
        ramie2 = new Box(0.5f, 0.1f, 0.1f, wyglad_ramie2);
        tg_ramie2.setTransform(T3d_ramie2);
        tg_ramie2.addChild(ramie2);
        TG_ramie2.addChild(tg_ramie2);
        TG_przegub2.addChild(TG_ramie2);

        //chywtak
        chwytak = new Cylinder(0.1f, 0.1f, wyglad_chwytak);
        T3d_chwytak.set(new Vector3f(1.2f, -0.9f, 0.0f));
        TG_chwytak.setTransform(T3d_chwytak);
        TG_chwytak.addChild(chwytak);
        TG_ramie2.addChild(TG_chwytak);

        //klocek
        klocek = new Box(0.1f, 0.1f, 0.1f, wyglad_klocek);
        T3d_klocek.set(new Vector3f(2f, 0.15f, -0.5f));
        TG_klocek.setTransform(T3d_klocek);
        TG_klocek.addChild(klocek);






          
        // światło kierunkowe 
        Color3f light1Color = new Color3f(0.5f, 0.3f, 0.4f);
        Vector3f light1Direction = new Vector3f(4.0f, -7.0f, -12.0f);
        DirectionalLight light1 = new DirectionalLight(light1Color, light1Direction);
        light1.setInfluencingBounds(bounds);
        scena.addChild(light1);

        

        // światło
        Color3f ambientColor = new Color3f(1.0f, 1.0f, 1.0f);
        AmbientLight ambientLight = new AmbientLight(ambientColor);
        ambientLight.setInfluencingBounds(bounds);
        scena.addChild(ambientLight);


        scena.addChild(TG_podloze);
        scena.addChild(TG_klocek);
        scena.addChild(niebo);
        scena.compile();
        return scena;

    }

    private void pozycjaKamery(Vector3f wektor) {

        OrbitBehavior orbit = new OrbitBehavior(canvas, OrbitBehavior.REVERSE_ROTATE);
        BoundingSphere bounds = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100.0);
        orbit.setSchedulingBounds(bounds);
        vPlatform = s_univ.getViewingPlatform();
        Transform3D temp = new Transform3D();
        temp.set(wektor);
        vPlatform.getViewPlatformTransform().setTransform(temp);
        vPlatform.setViewPlatformBehavior(orbit);
    }

    private void utworzScene() {

        jPanel = new JPanel();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);


        GroupLayout jPanel1Layout = new GroupLayout(jPanel);
        jPanel.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGap(0, 1000, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGap(0, 600, Short.MAX_VALUE)
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGap(0, 0,0)
                        .addComponent(jPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(35, Short.MAX_VALUE))
        );

        pack();

        setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        BranchGroup scena = utworzRobota();
        scena.compile();

        s_univ.addBranchGraph(scena);
     
        panel.add(canvas);
        panel.setSize(jPanel.getWidth(), jPanel.getHeight());
        jPanel.add(panel);

        createBufferStrategy(2);
    }


    @Override
    public void keyPressed(KeyEvent ke) {

        if (obslugaKlawiszy(ke.getKeyCode())) {
        
        if (CzyNagrywa) {
                nagranie.add(new PozycjaRobota(α_podstawa, α_przegub, α_przegub2, trzyma));
            }
        }
            //klawisz_ruchu = ke.getKeyCode();
            
       if (ke.getKeyCode() == KeyEvent.VK_N) {
            text.setText("nagrywanie");
            CzyNagrywa = true;
            nagranie.clear();
            klatka = 0;
            System.out.println("nagrywanie ");
             }
          
           if (ke.getKeyCode() == KeyEvent.VK_M) {
            text.setText("odtwarzanie");
            CzyNagrywa = false;
            odtworz_ruch = true;
        } 
           if (ke.getKeyCode() == KeyEvent.VK_ESCAPE){
               odtworz_ruch = !odtworz_ruch;
               text.setText("swobodne poruszanie");
           }
        }
    
    @Override
    public void keyReleased(KeyEvent ke) {
    }

    @Override
    public void keyTyped(KeyEvent ke) {
    }

    public boolean obslugaKlawiszy(int klawisz_ruchu) {

        //sprawdzenie pozycji chwytaka i klocka
        Vector3f pos_chwyt = new Vector3f();
        T3d_chwytak.get(pos_chwyt);

        Vector3f pos_kloc = new Vector3f();
        T3d_klocek.get(pos_kloc);

        if(klawisz_ruchu == KeyEvent.VK_SPACE && pos_chwyt.x-pos_kloc.x<0.2f && pos_chwyt.y-pos_kloc.y<0.2f && pos_chwyt.z-pos_kloc.z<0.2f && pos_chwyt.x-pos_kloc.x>-0.2f && pos_chwyt.y-pos_kloc.y>-0.2f && pos_chwyt.z-pos_kloc.z>-0.2f)
            trzyma = !trzyma;


        // obrot podstawy odwrotnie do wskazowek zegara
        if (klawisz_ruchu == KeyEvent.VK_LEFT) {
            α_podstawa += ruch;
            if(trzyma == false && pos_chwyt.x-pos_kloc.x<0.2f && pos_chwyt.y-pos_kloc.y<0.2f && pos_chwyt.z-pos_kloc.z<0.2f && pos_chwyt.x-pos_kloc.x>-0.2f && pos_chwyt.y-pos_kloc.y>-0.2f && pos_chwyt.z-pos_kloc.z>-0.2f)
                α_podstawa -= 2*ruch;
            System.out.println(pos_chwyt);
            System.out.println(pos_kloc);
            System.out.println(trzyma);
            return true;
        }

        // obrot podstawy zgodnie ze wskazowkami zegara
        if (klawisz_ruchu == KeyEvent.VK_RIGHT) {
            α_podstawa -= ruch;
            if(trzyma == false && pos_chwyt.x-pos_kloc.x<0.2f && pos_chwyt.y-pos_kloc.y<0.2f && pos_chwyt.z-pos_kloc.z<0.2f && pos_chwyt.x-pos_kloc.x>-0.2f && pos_chwyt.y-pos_kloc.y>-0.2f && pos_chwyt.z-pos_kloc.z>-0.2f)
                α_podstawa += 2*ruch;
            System.out.println(pos_chwyt);
            System.out.println(pos_kloc);
            return true;
        }

        //ruch przegubu w dol
        if (klawisz_ruchu == KeyEvent.VK_DOWN) {
            if (α_przegub < Math.PI / 8 ) {
                α_przegub += ruch;
                if(pos_chwyt.y<0.15 || (trzyma == false && pos_chwyt.x-pos_kloc.x<0.2f && pos_chwyt.y-pos_kloc.y<0.2f && pos_chwyt.z-pos_kloc.z<0.2f && pos_chwyt.x-pos_kloc.x>-0.2f && pos_chwyt.y-pos_kloc.y>-0.2f && pos_chwyt.z-pos_kloc.z>-0.2f))
                    α_przegub -= 2*ruch;
                System.out.println(pos_chwyt);
                System.out.println(pos_kloc);
                return true;
            }
        }

        //ruch przegubu w gore
        if (klawisz_ruchu == KeyEvent.VK_UP) {
            if (α_przegub > -Math.PI / 6) {
                α_przegub -= ruch;
                System.out.println(pos_chwyt);
                System.out.println(pos_kloc);
                return true;
            }
        }

        //ruch przegubu2 w gore
        if (klawisz_ruchu == KeyEvent.VK_W) {
            if (α_przegub2 < Math.PI / 3 ) {
                α_przegub2 += ruch;
                if(pos_chwyt.y<0.15 || (trzyma == false && pos_chwyt.x-pos_kloc.x<0.2f && pos_chwyt.y-pos_kloc.y<0.2f && pos_chwyt.z-pos_kloc.z<0.2f && pos_chwyt.x-pos_kloc.x>-0.2f && pos_chwyt.y-pos_kloc.y>-0.2f && pos_chwyt.z-pos_kloc.z>-0.2f))
                    α_przegub2 -= ruch;
                System.out.println(pos_chwyt);
                System.out.println(pos_kloc);
                return true;
            }
        }

        //ruch przegubu2 w dol
        if (klawisz_ruchu == KeyEvent.VK_S) {
            if (α_przegub2 > -Math.PI / 3 ) {
                α_przegub2 -= ruch;
                if(pos_chwyt.y<0.15 || (trzyma == false && pos_chwyt.x-pos_kloc.x<0.2f && pos_chwyt.y-pos_kloc.y<0.2f && pos_chwyt.z-pos_kloc.z<0.2f && pos_chwyt.x-pos_kloc.x>-0.2f && pos_chwyt.y-pos_kloc.y>-0.2f && pos_chwyt.z-pos_kloc.z>-0.2f))
                    α_przegub2 += ruch;
                System.out.println(pos_chwyt);
                System.out.println(pos_kloc);
                return true;
            }
        }



        return false;
    }

    class OdegranieRuchu extends TimerTask {

        @Override
        public void run() {
            if (!odtworz_ruch || nagranie.isEmpty())
                return;
            if (klatka >= nagranie.size()){
                klatka = 0;
               
            }
            PozycjaRobota NumerKlatki = nagranie.get(klatka);
            α_podstawa = NumerKlatki.α_podstawa;
            α_przegub = NumerKlatki.α_przegub;
            α_przegub2 = NumerKlatki.α_przegub2;
            trzyma = NumerKlatki.trzyma;
            klatka++;
            
            
           
        }
    }
    
        class PozycjaRobota {
        
        float α_podstawa;
        float α_przegub;
        float α_przegub2;
        boolean trzyma;
        
            public PozycjaRobota(float α_podstawa, float α_przegub, float α_przegub2, boolean trzyma) {
                this.α_podstawa = α_podstawa;
                this.α_przegub = α_przegub;
                this.α_przegub2 = α_przegub2;
                this.trzyma = trzyma;
            }
                                      
    
    }


    private class Poruszanie extends TimerTask {

        @Override
        public void run() {

            RotInt_podstawa.setMinimumAngle(α_podstawa);
            RotInt_podstawa.setMaximumAngle(α_podstawa);
            RotInt_przegub.setMinimumAngle(α_przegub);
            RotInt_przegub.setMaximumAngle(α_przegub);
            RotInt_przegub2.setMinimumAngle(α_przegub2);
            RotInt_przegub2.setMaximumAngle(α_przegub2);


            chwytak.getLocalToVworld(T3d_chwytak);
            przegub2.getLocalToVworld(T3d_przegub2);
            TG_klocek.setTransform(T3d_klocek);

            Vector3f pos_kloc = new Vector3f();
            T3d_klocek.get(pos_kloc);
            Vector3f pos = new Vector3f();
            T3d_chwytak.get(pos);
            Vector3f ramie_pos = new Vector3f();
            T3d_przegub2.get(ramie_pos);

            if(trzyma){
                pos_kloc.x = pos.x;
                pos_kloc.y = pos.y;
                pos_kloc.z = pos.z;
                T3d_klocek.setTranslation(new Vector3f(pos_kloc.x, pos_kloc.y, pos_kloc.z));
            }
            if(!trzyma && pos_kloc.y>0.15f){

                pos_kloc.y -= 0.04;

                T3d_klocek.setTranslation(new Vector3f(pos_kloc.x, pos_kloc.y, pos_kloc.z));
            }

        }
    }


}
