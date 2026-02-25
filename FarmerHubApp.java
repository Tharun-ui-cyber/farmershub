import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.Border;

public class FarmerHubApp extends JFrame {
    CardLayout cardLayout;
    JPanel mainPanel;
    
    // --- Data Persistence File Name ---
    private static final String DATA_FILE = "farmerhub_users.ser"; 

    // --- Data Storage (User Authentication) ---
    private static final HashMap<String, UserData> userDatabase = new HashMap<>();
    private UserData currentUser = null; // Changed to non-static as it's session-specific

    // --- Application Data Models ---
    private static List<Product> productList = new ArrayList<>();
    private List<CartItem> cartList = new ArrayList<>();
    
    // UI components that need global access for updates
    JLabel cartCountLabel;
    
    // --- Global Constants for Validation ---
    // Password must contain at least one uppercase, one digit and one special char and be 8+ chars
    private static final String PASSWORD_REGEX = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=]).{8,}$";
    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    // --- Application Data Structures ---
    
    // User Data (Previous feature)
    static class UserData implements Serializable {
        private static final long serialVersionUID = 2L; 
        String username;
        String password;
        String email; 
        
        public UserData(String username, String password, String email) {
            this.username = username;
            this.password = password;
            this.email = email;
        }
    }

    // New: Product Data Model
    static class Product implements Serializable {
        private static final long serialVersionUID = 3L;
        String name;
        String description;
        String category;
        double price; // Price per unit/kg
        String listedBy; // Username of seller
        
        public Product(String name, String description, String category, double price, String listedBy) {
            this.name = name;
            this.description = description;
            this.category = category;
            this.price = price;
            this.listedBy = listedBy;
        }
    }
    
    // New: Cart Item Model (In-memory for session)
    static class CartItem {
        Product product;
        int quantity;
        
        public CartItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }
        
        public double getTotalPrice() {
            return product.price * quantity;
        }
    }

    // UI Components (fields so updateLanguage can access)
    JLabel appTitle;        
    JLabel tagline;         
    JComboBox<String> langSelector;

    // Login/Signup labels & buttons (keeping previous features)
    JTextField loginUserField; 
    JPasswordField loginPassField; 
    JLabel loginTitle, loginUserLabel, loginPassLabel;
    JButton loginBtn, signupBtn;
    // NEW: Forgot Password Button
    JButton forgotPassBtn;
    
    JLabel signupTitle, signupUserLabel, signupPassLabel, signupConfirmPassLabel, signupEmailLabel;
    JTextField signupUserField, signupEmailField; 
    JPasswordField signupPassField, signupConfirmPassField; 
    JButton registerBtn, backBtn;
    JLabel emailReqLabel, passReqLabel;

    // Dashboard Screen
    JLabel dashTitle;
    JButton feature1Btn, feature2Btn, feature3Btn, feature4Btn, feature5Btn; 

    // Languages and Translations (Keeping previous features)
    String[] languages = {"English", "తెలుగు", "தமிழ்", "हिन्दी"};
    HashMap<String, String[]> translations = new HashMap<>();

    public FarmerHubApp() {
        setTitle("FarmerHub");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveUsersToFile(); 
                System.exit(0);
            }
        });

        setExtendedState(MAXIMIZED_BOTH);

        // 1. Load User Data (Previous feature)
        loadUsersFromFile();
        if (userDatabase.isEmpty()) {
            userDatabase.put("farmer", new UserData("farmer", "Pass123!", "farm@hub.com"));
        }
        
        // 2. Initialize Product List (New feature)
        initializeProducts();
        
        // 3. Initialize Translations (Previous feature)
        initializeTranslations();


        // --- Layout Setup ---
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Top bar (app title only)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(230, 250, 230));
        appTitle = new JLabel("", SwingConstants.CENTER);
        appTitle.setFont(getEmojiOrUnicodeFont(Font.BOLD, 28));
        appTitle.setForeground(new Color(0, 100, 0));
        topPanel.add(appTitle, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        add(mainPanel, BorderLayout.CENTER);

        // Initialize JMenuBar globally
        JMenuBar globalMenuBar = new JMenuBar();
        setJMenuBar(globalMenuBar);

        // Initialize reusable language selector component
        langSelector = new JComboBox<>(languages);
        langSelector.setFont(getUnicodeFont(Font.PLAIN, 14));
        langSelector.addActionListener(e -> updateLanguage());

        // Add pages to card layout
        mainPanel.add(loginPanel(), "login");
        mainPanel.add(signupPanel(), "signup");
        mainPanel.add(dashboardPanel(), "dashboard");
        
        // --- NEW Functional Panels ---
        mainPanel.add(marketPlacePanel(), "marketplace");
        mainPanel.add(productListingPanel(), "sell_product");
        mainPanel.add(cartPanel(), "cart"); // Index 5
        mainPanel.add(profilePanel(), "profile");
        mainPanel.add(createStaticFeaturePanel("My Posts"), "myposts");
        mainPanel.add(createStaticFeaturePanel("WhatsApp Connect"), "whatsapp");
        mainPanel.add(createStaticFeaturePanel("Market Stats"), "stats");

        setVisible(true);
        updateLanguage();
    }
    
    private void initializeProducts() {
        // Initial dummy products for the marketplace
        productList.add(new Product("Organic Apples", "Freshly picked Himalayan apples.", "Fruits", 150.0, "vendor1"));
        productList.add(new Product("Farm Tomatoes", "Juicy red tomatoes from local farm.", "Vegetables", 35.0, "vendor2"));
        productList.add(new Product("Basmati Rice (10kg)", "Aged Basmati rice, premium quality.", "Grains", 800.0, "vendor3"));
        productList.add(new Product("Bananas (Dwarf Cavendish)", "Sweet and nutritious bananas.", "Fruits", 60.0, "vendor1"));
        productList.add(new Product("Spinach (Palak)", "Leafy green spinach, 1kg bundle.", "Vegetables", 40.0, "vendor2"));
        productList.add(new Product("Wheat Flour (Atta)", "Whole wheat atta, 5kg bag.", "Grains", 250.0, "vendor3"));
    }
    
    private void initializeTranslations() {
        translations.put("AppTitle", new String[]{"FarmerHub 🌱", "ఫార్మర్‌హబ్ 🌱", "ஃபார்மர்ஹப் 🌱", "फार्मरहब 🌱"});
        translations.put("Tagline", new String[]{"Connecting Farmers with Opportunities", "రైతులు అవకాశాలతో కలవ్వండి", "விவசாயிகளை வாய்ப்புகளுடன் இணைப்பு", "किसानों को अवसरों से जोड़ना"});
        translations.put("LoginTitle", new String[]{"Login Page", "లాగిన్ పేజీ", "உள்நுழைவு பக்கம்", "लॉगिन पृष्ठ"});
        translations.put("Username", new String[]{"Username:", "వినియోగదారు పేరు:", "பயனர் பெயர்:", "उपयोगकर्ता नाम:"});
        translations.put("Password", new String[]{"Password:", "పాస్వర్డ్:", "கடவுச்சொால்:", "पासवर्ड:"});
        translations.put("LoginBtn", new String[]{"Login", "లాగిన్", "உள்நுழைவு", "लॉगिन"});
        translations.put("SignupBtn", new String[]{"Sign Up", "సైన్ అప్", "பதிவு செய்யவும்", "साइन अप"});
        
        // NEW Forgot Password Translations
        translations.put("ForgotPassBtn", new String[]{"Forgot Password?", "పాస్వర్డ్ మరచిపోయారా?", "கடவுச்சொல்லை மறந்துவிட்டீர்களா?", "पासवर्ड भूल गए?"});
        translations.put("ResetTitle", new String[]{"Password Reset (Simulated)", "పాస్‌వర్డ్ రీసెట్ (అనుకరణ)", "கடவுச்சொல் மீட்டமைப்பு (போலி)", "पासवर्ड रीसेट (सिम्युలేటెड)"});
        translations.put("ResetPrompt", new String[]{"Enter your Username or Email:", "మీ వినియోగదారు పేరు లేదా ఈమెయిల్ నమోదు చేయండి:", "உங்கள் பயனர் பெயர் அல்லது மின்னஞ்சலை உள்ளிடவும்:", "अपना उपयोगकर्ता नाम या ईमेल दर्ज करें:"});
        translations.put("ResetSuccess", new String[]{"If the username/email is correct, a password reset link has been simulated to be sent to your registered email. Check your registered email associated with your Gmail/Domain.", "వినియోగదారు పేరు/ఈమెయిల్ సరైనదైతే, మీ నమోదిత ఈమెయిల్‌కు పాస్‌వర్డ్ రీసెట్ లింక్ పంపబడినట్లు అనుకరణ చేయబడింది. మీ Gmail/డొమైన్‌కు సంబంధించిన నమోదిత ఈమెయిల్ తనిఖీ చేయండి.", "பயனர் பெயர்/மின்னஞ்சல் சரியாக இருந்தால், உங்கள் பதிவு செய்யப்பட்ட மின்னஞ்சலுக்கு கடவுச்சொல் மீட்டமைப்பு இணைப்பு அனுப்பப்பட்டதாக போலியாகக் காட்டப்பட்டுள்ளது. உங்கள் Gmail/டொமைனுடன் தொடர்புடைய பதிவு செய்யப்பட்ட மின்னஞ்சலைச் சரிபார்க்கவும்.", "यदि उपयोगकर्ता नाम/ईमेल सही है, तो आपके पंजीकृत ईमेल पर एक पासवर्ड रीसेट लिंक भेजे जाने का अनुकरण किया गया है। अपने Gmail/डोमेन से जुड़े पंजीकृत ईमेल की जाँच करें।"});
        translations.put("UserNotFound", new String[]{"User not found. Please try again or sign up.", "వినియోగదారు కనుగొనబడలేదు. దయచేసి మళ్లీ ప్రయత్నించండి లేదా నమోదు చేయండి.", "பயனர் காணப்படவில்லை. மீண்டும் முயற்சிக்கவும் அல்லது பதிவு செய்யவும்.", "उपयोगकर्ता नहीं मिला। कृपया पुन: प्रयास करें या साइन अप करें।"});
        
        translations.put("SignupTitle", new String[]{"Create Account", "ఖాతా సృష్టించండి", "கணக்கை உருவாக்க", "खाता बनाएं"});
        translations.put("NewUser", new String[]{"New Username (min 4 chars):", "కొత్త వినియోగదారు పేరు (కనీసం 4 అక్షరాలు):", "புதிய பயனர் பெயர் (குறைந்தது 4 எழுத்துக்கள்):", "नया उपयोगकर्ता नाम (न्यूनतम 4 वर्ण):"});
        translations.put("Email", new String[]{"Email:", "ఈమెయిల్:", "மின்னஞ்சல்:", "ईमेल:"});
        translations.put("NewPass", new String[]{"New Password:", "కొత్త పాస్వర్డ్:", "புதிய கடவுச்சொல்:", "नया पासवर्ड:"});
        translations.put("ConfirmPass", new String[]{"Confirm Password:", "పాస్‌వర్డ్‌ని నిర్ధారించండి:", "கடவுச்சொல்லை உறுதிப்படுத்து:", "पासवर्ड की पुष्टि करें:"});
        translations.put("RegisterBtn", new String[]{"Register", "నమోదు", "பதிவு", "रजिस्टर"});
        translations.put("BackBtn", new String[]{"Back to Login", "లాగిన్‌కి వెళ్లండి", "உள்நுழைவுக்குத் திரும்பு", "लॉगिन पर वापस जाएं"});
        
        translations.put("DashboardTitle", new String[]{"Welcome to Farmer Hub", "ఫార్మర్ హబ్‌కు స్వాగతం", "ஃபார்மர் ஹப்பிற்கு வரவேற்கிறோம்", "फार्मर हब में आपका स्वागत है"});
        translations.put("ProfileBtn", new String[]{"My Profile 👤", "నా ప్రొఫైల్ 👤", "எனது சுயவிவரம் 👤", "मेरा प्रोफाइल 👤"});
        translations.put("CartBtn", new String[]{"My Cart 🛒", "నా కార్ట్ 🛒", "எனது வண்டி 🛒", "मेरा कार्ट 🛒"});
        
        translations.put("Feature1", new String[]{"Buy Products 💰", "ఉత్పత్తులు కొనండి 💰", "பொருட்களை வாங்கவும் 💰", "उत्पाद खरीदें 💰"});
        translations.put("Feature2", new String[]{"Sell Harvest 🧑‍🌾", "పంట అమ్మండి 🧑‍🌾", "விளைபொருளை விற்கவும் 🧑‍🌾", "फसल बेचें 🧑‍🌾"});
        translations.put("Feature3", new String[]{"My Posts 📝", "నా పోస్ట్‌లు 📝", "எனது இடுகைகள் 📝", "मेरे पोस्ट 📝"});
        translations.put("Feature4", new String[]{"WhatsApp Connect 📞", "వాట్సాప్ కనెక్ట్ 📞", "வாட்ஸ்அப் இணைப்பு 📞", "व्हाट्सप्प कनेक्ट 📞"});
        translations.put("Feature5", new String[]{"Market Stats 📈", "మార్కెట్ గణాంకాలు 📈", "சந்தை புள்ளிவிவரங்கள் 📈", "बाज़ार आँकड़े 📈"});

        translations.put("LogoutBtn", new String[]{"Logout", "లాగ్ఔట్", "வெளியேறு", "लॉग आउट"});
        translations.put("Help", new String[]{"Help", "సహాయం", "உதவி", "मदद"});
        translations.put("Settings", new String[]{"Settings", "సెట్టింగ్‌లు", "அமைப்புகள்", "सेटिंग्स"});
        
        translations.put("PassReq", new String[]{"Password Rule:", "పాస్‌వర్డ్ నియమం:", "கடவுச்சொல் விதி:", "पासवर्ड नियम:"});
        translations.put("PassRule", new String[]{"8+ chars, Uppercase, Digit, Special Char (@#$%^&+=).", "8+ అక్షరాలు, పెద్ద అక్షరం, అంకె, ప్రత్యేక అక్షరం (@#$%^&+=).", "8+ எழுத்துக்கள், பெரிய எழுத்து, இலக்கம், சிறப்பு எழுத்து (@#$%^&+=).", "8+ वर्ण, अपरकेस, अंक, विशेष वर्ण (@#$%^&+=)."});
        translations.put("EmailReq", new String[]{"Email must be in a valid format (e.g., user@domain.com).", "ఈమెయిల్ సరైన ఫార్మాట్‌లో ఉండాలి (ఉదా: user@domain.com).", "மின்னஞ்சல் ஒரு சரியான வடிவமைப்பில் இருக்க வேண்டும் (எ.கா., user@domain.com).", "ईमेल एक वैध प्रारूप में होना चाहिए (उदाहरण के लिए, user@domain.com)।"});
        
        // NEW Marketplace Translations
        translations.put("MarketplaceTitle", new String[]{"Buy Products: Marketplace", "ఉత్పత్తులు కొనండి: మార్కెట్", "பொருட்களை வாங்கவும்: சந்தை", "उत्पाद खरीदें: बाज़ार"});
        translations.put("Category", new String[]{"Filter by Category:", "వర్గం ద్వారా ఫిల్టర్ చేయండి:", "வகைப்படி வடிகட்டவும்:", "श्रेणी के अनुसार फ़िल्टर करें:"});
        translations.put("All", new String[]{"All Products", "అన్ని ఉత్పత్తులు", "அனைத்து பொருட்கள்", "सभी उत्पाद"});
        translations.put("Fruits", new String[]{"Fruits 🍎", "పండ్లు 🍎", "பழங்கள் 🍎", "फल 🍎"});
        translations.put("Vegetables", new String[]{"Vegetables 🥬", "కూరగాయలు 🥬", "காய்கறிகள் 🥬", "सब्जियां 🥬"});
        translations.put("Grains", new String[]{"Grains 🌾", "ధాన్యాలు 🌾", "தானியங்கள் 🌾", "अनाज 🌾"});
        translations.put("AddCart", new String[]{"Add to Cart", "కార్ట్‌కు జోడించండి", "வண்டியில் சேர்க்கவும்", "कार्ट में जोड़ें"});
        translations.put("UnitPrice", new String[]{"Price (per kg/unit):", "ధర (ఒక కిలో/యూనిట్‌కు):", "விலை (ஒரு கிலோ/யூனிட்டிற்கு):", "कीमत (प्रति किलो/यूनिट):"});

        // NEW Sell Product Translations
        translations.put("SellTitle", new String[]{"Sell Harvest: List Product", "పంట అమ్మండి: ఉత్పత్తిని జాబితా చేయండి", "விளைபொருளை விற்கவும்: பொருட்களை பட்டியலிடவும்", "उत्पाद बिक्री के लिए सूचीबद्ध करें"});
        translations.put("ProductName", new String[]{"Product Name:", "ఉత్పత్తి పేరు:", "பொருளின் பெயர்:", "उत्पाद का नाम:"});
        translations.put("Description", new String[]{"Description:", "వివరణ:", "விளக்கம்:", "विवरण:"});
        translations.put("ListBtn", new String[]{"List Product for Sale", "అమ్మకం కోసం ఉత్పత్తిని జాబితా చేయండి", "விற்பனைக்கு பொருட்களை பட்டியலிடவும்", "उत्पाद बिक्री के लिए सूचीबद्ध करें"});
        
        // NEW Cart Translations
        translations.put("CartTitle", new String[]{"Your Shopping Cart", "మీ షాపింగ్ కార్ట్", "உங்கள் ஷாப்பிங் வண்டி", "आपका शॉपिंग कार्ट"});
        translations.put("Item", new String[]{"Item", "వస్తువు", "பொருள்", "वस्तु"});
        translations.put("Qty", new String[]{"Qty", "పరిమాణం", "అளவு", "मात्रा"});
        // FIX: Added missing key for the Cart table price column header
        translations.put("PriceCol", new String[]{"Price", "ధర", "விலై", "कीमत"}); 
        translations.put("Total", new String[]{"Total", "మొత్తం", "மொத்தம்", "कुल"});
        translations.put("Subtotal", new String[]{"Subtotal:", "ఉపమొత్తం:", "துணை மொத்தம்:", "उपयोगితా राशि:"});
        translations.put("CheckoutBtn", new String[]{"Checkout", "చెకౌట్", "செக் அவுட்", "चेक आउट"});
        translations.put("EmptyCart", new String[]{"Your cart is empty.", "మీ కార్ట్ ఖాళీగా ఉంది.", "உங்கள் வண்டி காலியாக உள்ளது.", "आपका कार्ट खाली है।"});
        
        // NEW Profile Translations
        translations.put("ProfileTitle", new String[]{"My Profile", "నా ప్రొఫైల్", "எனது சுயவிவரம்", "मेरा प्रोफाइल"});
        translations.put("RegisteredEmail", new String[]{"Registered Email:", "నమోదిత ఈమెయిల్:", "பதிவு செய்யப்பட்ட மின்னஞ்சల్:", "पंजीकृत ईमेल:"});
    }
    
    // ------------------------------------------------------------------
    // *** Data Persistence Methods (Keeping existing functionality) ***
    // ------------------------------------------------------------------

    private void loadUsersFromFile() {
        try (FileInputStream fis = new FileInputStream(DATA_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            @SuppressWarnings("unchecked")
            HashMap<String, UserData> loadedMap = (HashMap<String, UserData>) ois.readObject();
            userDatabase.clear();
            userDatabase.putAll(loadedMap); 

        } catch (FileNotFoundException e) {
            System.out.println("Data file not found. Starting with default user.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading user data: " + e.getMessage());
        }
    }

    private void saveUsersToFile() {
        try (FileOutputStream fos = new FileOutputStream(DATA_FILE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(userDatabase);

        } catch (IOException e) {
            System.err.println("Error saving user data: " + e.getMessage());
        }
    }
    
    // --- Utility Methods (Font, Gradient, Style, Validation) ---
    // (Keeping existing utility methods for consistency)
    
    private boolean isValidPassword(String password) {
        return Pattern.matches(PASSWORD_REGEX, password);
    }
    
    private boolean isValidEmail(String email) {
        return Pattern.matches(EMAIL_REGEX, email);
    }

    private Font getEmojiOrUnicodeFont(int style, int size) {
        String[] emojiFonts = {"Segoe UI Emoji", "Noto Color Emoji", "Apple Color Emoji", "EmojiOne Color"};
        for (String f : emojiFonts) {
            try {
                Font font = new Font(f, style, size);
                if (font.canDisplayUpTo("☰") == -1) return font;
            } catch (Exception ignored) {}
        }
        try {
            Font f = new Font("Nirmala UI", style, size);
            if (f.canDisplayUpTo("తెలుగు தமிழ் हिन्दी") == -1) return f;
        } catch (Exception ignored) {}
        return new Font("SansSerif", style, size);
    }

    private Font getUnicodeFont(int style, int size) {
        try {
            Font f = new Font("Nirmala UI", style, size);
            if (f.canDisplayUpTo("తెలుగు தமிழ் हिन्दी") == -1) return f;
        } catch (Exception ignored) {}
        return new Font("SansSerif", style, size);
    }

    class GradientPanel extends JPanel {
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            GradientPaint gp = new GradientPaint(0, 0, Color.WHITE, 0, getHeight(), new Color(204, 255, 204));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    private JPanel createPasswordFieldWithToggle(JPasswordField field) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        Border defaultBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1, true),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        );
        field.setBorder(defaultBorder);
        field.setFont(getUnicodeFont(Font.PLAIN, 14));
        
        JButton toggleBtn = new JButton("👁"); 
        toggleBtn.setFont(getEmojiOrUnicodeFont(Font.PLAIN, 14));
        toggleBtn.setPreferredSize(new Dimension(40, field.getPreferredSize().height));
        toggleBtn.setMargin(new Insets(0, 0, 0, 0));
        toggleBtn.setFocusPainted(false);
        toggleBtn.setBackground(new Color(240, 240, 240));

        toggleBtn.addActionListener(e -> {
            if (field.getEchoChar() == 0) {
                field.setEchoChar('•');
                toggleBtn.setText("👁");
            } else {
                field.setEchoChar((char) 0);
                toggleBtn.setText("🙈");
            }
        });
        
        field.setEchoChar('•');
        
        panel.add(field, BorderLayout.CENTER);
        panel.add(toggleBtn, BorderLayout.EAST);
        
        return panel;
    }
    
    private JTextField roundedTextField() {
        JTextField field = new JTextField(15);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1, true),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        field.setFont(getUnicodeFont(Font.PLAIN, 14));
        return field;
    }

    private JButton styledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(getUnicodeFont(Font.BOLD, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(color.darker()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(color); }
        });
        return btn;
    }

    private JPanel createFeatureButton(JButton btn, Color color) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(getUnicodeFont(Font.BOLD, 16));
        btn.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color.darker(), 1, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        wrapper.add(btn, BorderLayout.CENTER);
        return wrapper;
    }

    // --- Panel Creation Methods (Keeping Login/Signup for persistence) ---

    private JPanel loginPanel() {
        JPanel panel = new GradientPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel logo = new JLabel("🌱 FarmerHub", SwingConstants.CENTER);
        logo.setFont(getEmojiOrUnicodeFont(Font.BOLD, 28));
        logo.setForeground(new Color(0, 100, 0));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(logo, gbc);

        tagline = new JLabel("", SwingConstants.CENTER);
        tagline.setFont(getUnicodeFont(Font.ITALIC, 14));
        gbc.gridy = 1;
        panel.add(tagline, gbc);

        loginTitle = new JLabel();
        loginTitle.setFont(getUnicodeFont(Font.BOLD, 18));
        gbc.gridy = 2;
        panel.add(loginTitle, gbc);

        loginUserLabel = new JLabel(); 
        loginUserLabel.setFont(getUnicodeFont(Font.PLAIN, 14));
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(loginUserLabel, gbc);

        loginUserField = roundedTextField(); 
        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(loginUserField, gbc);

        loginPassLabel = new JLabel();
        loginPassLabel.setFont(getUnicodeFont(Font.PLAIN, 14));
        gbc.gridx = 0; gbc.gridy = 4;
        panel.add(loginPassLabel, gbc);

        loginPassField = new JPasswordField(15); 
        gbc.gridx = 1; gbc.gridy = 4;
        panel.add(createPasswordFieldWithToggle(loginPassField), gbc);
        
        loginBtn = styledButton("", new Color(76, 175, 80));
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1;
        panel.add(loginBtn, gbc);

        gbc.gridx = 1; gbc.gridy = 5;
        panel.add(langSelector, gbc);

        // NEW: Forgot Password Button
        forgotPassBtn = styledButton("", new Color(120, 120, 120)); // Grey for secondary action
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2; // Row 6, full width
        panel.add(forgotPassBtn, gbc);
        
        // Sign Up Button (Moved to row 7)
        signupBtn = styledButton("", new Color(33, 150, 243));
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        panel.add(signupBtn, gbc);
        
        // --- Action Listeners ---
        
        loginBtn.addActionListener(e -> {
            String identifier = loginUserField.getText().trim();
            String password = new String(loginPassField.getPassword());
            
            UserData foundUser = userDatabase.get(identifier.toLowerCase());

            if (foundUser == null) {
                JOptionPane.showMessageDialog(this, "User not found!", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (foundUser.password.equals(password)) {
                currentUser = foundUser;
                // Update UI dependent on current user (like profile)
                updateProfilePanel();
                if (getJMenuBar() != null) getJMenuBar().setVisible(true);
                cardLayout.show(mainPanel, "dashboard");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        forgotPassBtn.addActionListener(e -> handleForgotPassword()); // Handle the reset request

        signupBtn.addActionListener(e -> {
            if (signupPassField != null) signupPassField.setText("");
            if (signupConfirmPassField != null) signupConfirmPassField.setText("");
            cardLayout.show(mainPanel, "signup");
        });

        return panel;
    }
    
    /**
     * Handles the simulated "Forgot Password" functionality.
     * Prompts the user for their username or email and simulates sending a reset link.
     */
    private void handleForgotPassword() {
        int langIndex = Math.max(0, langSelector.getSelectedIndex());
        
        JLabel promptLabel = new JLabel("<html>" + translations.get("ResetPrompt")[langIndex] + "</html>");
        JTextField inputField = roundedTextField();
        
        JPanel resetPanel = new JPanel(new BorderLayout(5, 5));
        resetPanel.add(promptLabel, BorderLayout.NORTH);
        resetPanel.add(inputField, BorderLayout.CENTER);
        
        int option = JOptionPane.showConfirmDialog(
            this,
            resetPanel,
            translations.get("ResetTitle")[langIndex],
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            String identifier = inputField.getText().trim();
            if (identifier.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter your username or email.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            UserData foundUser = null;
            // 1. Search by Username
            if (userDatabase.containsKey(identifier.toLowerCase())) {
                foundUser = userDatabase.get(identifier.toLowerCase());
            } 
            
            // 2. Search by Email
            if (foundUser == null) {
                for (UserData user : userDatabase.values()) {
                    if (user.email.equalsIgnoreCase(identifier)) {
                        foundUser = user;
                        break;
                    }
                }
            }

            if (foundUser != null) {
                // SUCCESS: Simulate email sending
                String successMsg = translations.get("ResetSuccess")[langIndex];
                JOptionPane.showMessageDialog(this, successMsg, "Password Reset Initiated", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // FAILURE: User not found
                String failMsg = translations.get("UserNotFound")[langIndex];
                JOptionPane.showMessageDialog(this, failMsg, "Password Reset Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel signupPanel() {
        // ... (Signup Panel implementation remains the same for consistency) ...
        JPanel panel = new GradientPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        signupTitle = new JLabel();
        signupTitle.setFont(getUnicodeFont(Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.insets = new Insets(8,8,8,8); gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(signupTitle, gbc);

        // 1. Username
        signupUserLabel = new JLabel();
        signupUserLabel.setFont(getUnicodeFont(Font.PLAIN, 14));
        gbc.gridwidth = 1; gbc.gridx = 0; gbc.gridy = 1; gbc.insets = new Insets(8,8,4,8);
        panel.add(signupUserLabel, gbc);
        signupUserField = roundedTextField();
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(signupUserField, gbc);

        // 2. Email
        signupEmailLabel = new JLabel();
        signupEmailLabel.setFont(getUnicodeFont(Font.PLAIN, 14));
        gbc.gridx = 0; gbc.gridy = 2; gbc.insets = new Insets(8,8,4,8);
        panel.add(signupEmailLabel, gbc);
        signupEmailField = roundedTextField();
        gbc.gridx = 1; gbc.gridy = 2;
        panel.add(signupEmailField, gbc);
        
        // Email Requirement Label
        emailReqLabel = new JLabel(); 
        emailReqLabel.setFont(getUnicodeFont(Font.ITALIC, 11));
        emailReqLabel.setForeground(new Color(150, 0, 0)); 
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.insets = new Insets(0, 8, 12, 8); 
        panel.add(emailReqLabel, gbc);

        // 3. Password
        signupPassLabel = new JLabel();
        signupPassLabel.setFont(getUnicodeFont(Font.PLAIN, 14));
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.insets = new Insets(8,8,4,8);
        panel.add(signupPassLabel, gbc);
        signupPassField = new JPasswordField(15);
        gbc.gridx = 1; gbc.gridy = 4;
        panel.add(createPasswordFieldWithToggle(signupPassField), gbc);
        
        // Password Requirement Label
        passReqLabel = new JLabel(); 
        passReqLabel.setFont(getUnicodeFont(Font.ITALIC, 11));
        passReqLabel.setForeground(new Color(150, 0, 0)); 
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.insets = new Insets(0, 8, 12, 8); 
        panel.add(passReqLabel, gbc);

        // 4. Confirm Password
        signupConfirmPassLabel = new JLabel();
        signupConfirmPassLabel.setFont(getUnicodeFont(Font.PLAIN, 14));
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 1; gbc.insets = new Insets(8,8,8,8);
        panel.add(signupConfirmPassLabel, gbc);
        signupConfirmPassField = new JPasswordField(15);
        gbc.gridx = 1; gbc.gridy = 6;
        panel.add(createPasswordFieldWithToggle(signupConfirmPassField), gbc);
        
        // Buttons
        gbc.insets = new Insets(8, 8, 8, 8); 
        registerBtn = styledButton("", new Color(76, 175, 80));
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER; 
        panel.add(registerBtn, gbc);

        backBtn = styledButton("", new Color(158,158,158));
        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2; 
        panel.add(backBtn, gbc);

        registerBtn.addActionListener(e -> {
            String user = signupUserField.getText().trim();
            String email = signupEmailField.getText().trim();
            String pass = new String(signupPassField.getPassword());
            String confirmPass = new String(signupConfirmPassField.getPassword());
            
            if (user.isEmpty() || email.isEmpty() || pass.isEmpty() || confirmPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            } 
            if (!pass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (user.length() < 4) {
                 JOptionPane.showMessageDialog(this, "Username must be at least 4 characters long!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                 return;
            }
            if (!isValidEmail(email)) {
                 JOptionPane.showMessageDialog(this, "Invalid email format. Please use a valid email (e.g., user@domain.com).", "Validation Error", JOptionPane.ERROR_MESSAGE);
                 return;
            }
            if (!isValidPassword(pass)) {
                 JOptionPane.showMessageDialog(this, "Password failed validation. It must be 8+ characters and contain at least one: Uppercase letter, Digit, and Special Character (@#$%^&+=).", "Password Check Failed", JOptionPane.ERROR_MESSAGE);
                 return;
            }
            if (userDatabase.containsKey(user.toLowerCase())) {
                JOptionPane.showMessageDialog(this, "User already exists!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            userDatabase.put(user.toLowerCase(), new UserData(user, pass, email));
            JOptionPane.showMessageDialog(this, "Registration Successful! Please Log In.", "Success", JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(mainPanel, "login");
            
            signupUserField.setText(""); signupEmailField.setText(""); signupPassField.setText(""); signupConfirmPassField.setText("");
            if (loginUserField != null) loginUserField.setText(user);
            if (loginPassField != null) loginPassField.setText("");
        });

        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "login"));

        return panel;
    }

    private JPanel dashboardPanel() {
        JPanel panel = new GradientPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- Top Header Panel (Title + Cart/Profile) ---
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setOpaque(false);

        dashTitle = new JLabel("", SwingConstants.CENTER);
        dashTitle.setFont(getUnicodeFont(Font.BOLD, 22));
        
        // --- Action Buttons (My Profile, My Cart) ---
        JPanel actionBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBtnPanel.setOpaque(false);
        
        JButton profileAccessBtn = styledButton("", new Color(121, 85, 72));
        profileAccessBtn.addActionListener(e -> cardLayout.show(mainPanel, "profile"));

        JButton cartAccessBtn = styledButton("", new Color(255, 87, 34));
        cartCountLabel = new JLabel("0");
        cartCountLabel.setForeground(Color.WHITE);
        cartCountLabel.setFont(getUnicodeFont(Font.BOLD, 14));
        
        // Combine Cart Button and Count Label
        JPanel cartButtonWrapper = new JPanel(new BorderLayout(5, 0));
        cartButtonWrapper.setOpaque(false);
        cartButtonWrapper.add(cartAccessBtn, BorderLayout.CENTER);
        cartButtonWrapper.add(cartCountLabel, BorderLayout.EAST);
        
        cartAccessBtn.addActionListener(e -> {
            updateCartPanel();
            cardLayout.show(mainPanel, "cart");
        });
        
        actionBtnPanel.add(profileAccessBtn);
        actionBtnPanel.add(cartButtonWrapper); // NOTE: Added wrapper here

        headerPanel.add(dashTitle, BorderLayout.WEST);
        headerPanel.add(actionBtnPanel, BorderLayout.EAST);
        
        // --- Main Feature Grid ---
        JPanel btnPanel = new JPanel(new GridLayout(3, 2, 25, 25)); 
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        feature1Btn = new JButton(); // Buy Products -> Marketplace
        feature2Btn = new JButton(); // Sell Harvest -> Product Listing
        feature3Btn = new JButton(); // My Posts
        feature4Btn = new JButton(); // WhatsApp Connect
        feature5Btn = new JButton(); // Market Stats

        btnPanel.add(createFeatureButton(feature1Btn, new Color(255, 193, 7))); 
        btnPanel.add(createFeatureButton(feature2Btn, new Color(244, 67, 54)));
        btnPanel.add(createFeatureButton(feature3Btn, new Color(33, 150, 243)));
        btnPanel.add(createFeatureButton(feature4Btn, new Color(37, 211, 102)));
        btnPanel.add(createFeatureButton(feature5Btn, new Color(96, 125, 139)));

        // Button Actions 
        feature1Btn.addActionListener(e -> cardLayout.show(mainPanel, "marketplace"));
        feature2Btn.addActionListener(e -> cardLayout.show(mainPanel, "sell_product"));
        feature3Btn.addActionListener(e -> cardLayout.show(mainPanel, "myposts"));
        feature4Btn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Redirecting to WhatsApp connect...", "Info", JOptionPane.INFORMATION_MESSAGE)); 
        feature5Btn.addActionListener(e -> cardLayout.show(mainPanel, "stats"));

        // Final assembly
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(btnPanel, BorderLayout.CENTER);
        
        // Update the menu bar
        setupDashboardMenu(profileAccessBtn, cartAccessBtn);
        
        return panel;
    }
    
    // --- Marketplace Panel (Buy Products) ---
    private JPanel marketPlacePanel() {
        JPanel panel = new GradientPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("", SwingConstants.CENTER);
        titleLabel.setFont(getUnicodeFont(Font.BOLD, 24));
        titleLabel.setName("MarketplaceTitle");
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setOpaque(false);
        
        // --- Category Buttons (Filter) ---
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        filterPanel.setOpaque(false);
        filterPanel.setBorder(BorderFactory.createTitledBorder(translations.get("Category")[0]));

        String[] categories = {"All", "Fruits", "Vegetables", "Grains"};
        for (String cat : categories) {
            JButton catBtn = styledButton(translations.get(cat)[0], new Color(0, 150, 136));
            catBtn.setActionCommand(cat);
            catBtn.addActionListener(e -> {
                filterProducts(mainContent, cat);
                updateLanguage(); // Update button text after filter
            });
            filterPanel.add(catBtn);
        }
        
        mainContent.add(filterPanel, BorderLayout.NORTH);
        
        // Initial product view
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        
        mainContent.add(scrollPane, BorderLayout.CENTER);
        filterProducts(mainContent, "All"); 
        
        panel.add(mainContent, BorderLayout.CENTER);
        panel.add(createBackButton("dashboard"), BorderLayout.SOUTH);
        
        return panel;
    }

    private void filterProducts(JPanel container, String category) {
        JScrollPane scrollPane = (JScrollPane) container.getComponent(1); // Get the JScrollPane
        
        JPanel productGrid = new JPanel(new GridLayout(0, 3, 15, 15)); // 3 columns, infinite rows
        productGrid.setOpaque(false);
        productGrid.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Clear existing components (to handle re-filtering)
        productGrid.removeAll();
        
        for (Product p : productList) {
            if (category.equals("All") || p.category.equals(category)) {
                productGrid.add(createProductCard(p));
            }
        }
        
        scrollPane.setViewportView(productGrid);
        container.revalidate();
        container.repaint();
    }
    
    private JPanel createProductCard(Product product) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 150), 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(Color.WHITE);

        JLabel nameLabel = new JLabel("<html><b>" + product.name + "</b></html>");
        nameLabel.setFont(getUnicodeFont(Font.BOLD, 14));
        card.add(nameLabel, BorderLayout.NORTH);

        JLabel descLabel = new JLabel("<html><i style='font-size: 10px;'>" + product.description + "</i></html>");
        descLabel.setFont(getUnicodeFont(Font.PLAIN, 12));
        card.add(descLabel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        southPanel.setOpaque(false);

        JLabel priceLabel = new JLabel(NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(product.price) + " / unit");
        priceLabel.setForeground(new Color(0, 100, 0));
        priceLabel.setFont(getUnicodeFont(Font.BOLD, 14));
        southPanel.add(priceLabel);

        JButton addBtn = styledButton(translations.get("AddCart")[0], new Color(255, 87, 34));
        addBtn.addActionListener(e -> addToCart(product));
        southPanel.add(addBtn);

        card.add(southPanel, BorderLayout.SOUTH);
        return card;
    }

    // --- Product Listing Panel (Sell Harvest) ---
    private JPanel productListingPanel() {
        JPanel panel = new GradientPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JLabel titleLabel = new JLabel("", SwingConstants.CENTER);
        titleLabel.setFont(getUnicodeFont(Font.BOLD, 24));
        titleLabel.setName("SellTitle");
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        
        // 1. Product Name
        JLabel nameLabel = new JLabel(translations.get("ProductName")[0] + ":");
        JTextField nameField = roundedTextField();
        nameLabel.setName("ProductName");
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3; formPanel.add(nameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.7; formPanel.add(nameField, gbc);

        // 2. Category
        JLabel categoryLabel = new JLabel(translations.get("Category")[0] + ":");
        String[] cats = {"Fruits", "Vegetables", "Grains"};
        JComboBox<String> categoryCombo = new JComboBox<>(cats);
        categoryCombo.setName("Category");
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3; formPanel.add(categoryLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.7; formPanel.add(categoryCombo, gbc);
        
        // 3. Price
        JLabel priceLabel = new JLabel(translations.get("UnitPrice")[0] + ":");
        JTextField priceField = roundedTextField();
        priceField.setToolTipText("Enter price per kg/unit (e.g., 150.0)");
        priceLabel.setName("UnitPrice");
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3; formPanel.add(priceLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.7; formPanel.add(priceField, gbc);

        // 4. Description
        JLabel descLabel = new JLabel(translations.get("Description")[0] + ":");
        JTextArea descArea = new JTextArea(4, 20);
        descArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        JScrollPane scrollDesc = new JScrollPane(descArea);
        descLabel.setName("Description");
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3; formPanel.add(descLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0.7; gbc.fill = GridBagConstraints.BOTH; formPanel.add(scrollDesc, gbc);
        
        // 5. Submit Button
        JButton listBtn = styledButton(translations.get("ListBtn")[0], new Color(76, 175, 80));
        listBtn.setName("ListBtn");
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 10, 10, 10);
        formPanel.add(listBtn, gbc);
        
        listBtn.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                String category = (String) categoryCombo.getSelectedItem();
                double price = Double.parseDouble(priceField.getText().trim());
                String description = descArea.getText().trim();
                String listedBy = currentUser.username;
                
                if (name.isEmpty() || description.isEmpty() || price <= 0) {
                    JOptionPane.showMessageDialog(this, "Please fill out all fields correctly.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                Product newProduct = new Product(name, description, category, price, listedBy);
                productList.add(newProduct);
                
                JOptionPane.showMessageDialog(this, "Product listed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                
                // Clear fields
                nameField.setText("");
                priceField.setText("");
                descArea.setText("");
                
                cardLayout.show(mainPanel, "dashboard"); // Return to dashboard
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Price must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(createBackButton("dashboard"), BorderLayout.SOUTH);
        
        return panel;
    }
    
    // --- Cart Panel (Cart) ---
    private JPanel cartPanel() {
        JPanel panel = new GradientPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("", SwingConstants.CENTER);
        titleLabel.setFont(getUnicodeFont(Font.BOLD, 24));
        titleLabel.setName("CartTitle");
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // Placeholder for dynamic cart content
        JPanel contentPanel = new JPanel(new BorderLayout()); 
        contentPanel.setOpaque(false);
        panel.add(contentPanel, BorderLayout.CENTER);
        
        JPanel checkoutPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        checkoutPanel.setOpaque(false);
        panel.add(checkoutPanel, BorderLayout.SOUTH);
        
        // Add components to panel on load via updateCartPanel
        
        return panel;
    }
    
    // Updates the cart content dynamically
    private void updateCartPanel() {
        // FIX: Replaced incorrect JPanel.indexOf call with correct component index (index 5)
        // login(0), signup(1), dashboard(2), marketplace(3), sell_product(4), cart(5)
        JPanel cartPanel = (JPanel) mainPanel.getComponent(5); // Index of "cart" panel
        
        JPanel contentPanel = (JPanel) cartPanel.getComponent(1); // Get content panel
        JPanel checkoutPanel = (JPanel) cartPanel.getComponent(2); // Get checkout panel

        contentPanel.removeAll();
        checkoutPanel.removeAll();
        
        if (cartList.isEmpty()) {
            JLabel emptyLabel = new JLabel(translations.get("EmptyCart")[0], SwingConstants.CENTER);
            emptyLabel.setFont(getUnicodeFont(Font.ITALIC, 16));
            contentPanel.add(emptyLabel, BorderLayout.CENTER);
        } else {
            // Setup Table Model
            String[] columnNames = {
                translations.get("Item")[0], 
                translations.get("Qty")[0], 
                translations.get("PriceCol")[0], 
                translations.get("Total")[0] 
            };
            Object[][] data = new Object[cartList.size()][4];
            
            // --- FIX START: Use a temp variable for mutable calculation ---
            double calculatedSum = 0; 
            
            for (int i = 0; i < cartList.size(); i++) {
                CartItem item = cartList.get(i);
                data[i][0] = item.product.name;
                data[i][1] = item.quantity;
                data[i][2] = NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(item.product.price);
                data[i][3] = NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(item.getTotalPrice());
                calculatedSum += item.getTotalPrice(); // CalculatedSum is mutated here
            }
            
            // Assign the final, calculated value to the effectively final variable 'subtotal'
            final double subtotal = calculatedSum; 
            // --- FIX END ---
            
            JTable table = new JTable(data, columnNames);
            table.setFont(getUnicodeFont(Font.PLAIN, 14));
            table.setRowHeight(25);
            table.getTableHeader().setFont(getUnicodeFont(Font.BOLD, 14));
            JScrollPane scrollPane = new JScrollPane(table);
            
            contentPanel.add(scrollPane, BorderLayout.CENTER);
            
            // Checkout UI
            JLabel subtotalLabel = new JLabel("<html><b>" + translations.get("Subtotal")[0] + "</b> " + NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(subtotal) + "</html>");
            subtotalLabel.setFont(getUnicodeFont(Font.BOLD, 16));
            
            JButton checkoutBtn = styledButton(translations.get("CheckoutBtn")[0], new Color(76, 175, 80));
            // The lambda now safely accesses the effectively final 'subtotal' variable.
            checkoutBtn.addActionListener(e -> {
                JOptionPane.showMessageDialog(this, "Checkout Successful! (Simulated) Total: " + NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(subtotal), "Order Placed", JOptionPane.INFORMATION_MESSAGE);
                cartList.clear(); // Clear cart after simulated checkout
                updateCartPanel();
                updateCartCount();
            });
            
            checkoutPanel.add(subtotalLabel);
            checkoutPanel.add(checkoutBtn);
        }
        
        cartPanel.add(createBackButton("dashboard"), BorderLayout.SOUTH);
        cartPanel.revalidate();
        cartPanel.repaint();
    }
    
    // --- Profile Panel (My Profile) ---
    private JPanel profilePanel() {
        JPanel panel = new GradientPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("", SwingConstants.CENTER);
        titleLabel.setFont(getUnicodeFont(Font.BOLD, 24));
        titleLabel.setName("ProfileTitle");
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        panel.add(infoPanel, BorderLayout.CENTER);
        
        // Content will be generated by updateProfilePanel
        
        panel.add(createBackButton("dashboard"), BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void updateProfilePanel() {
        // This panel is at index 6: login(0), signup(1), dashboard(2), marketplace(3), sell_product(4), cart(5), profile(6)
        JPanel profilePanel = (JPanel) mainPanel.getComponent(6); 
        JPanel infoPanel = (JPanel) profilePanel.getComponent(1);
        infoPanel.removeAll();
        
        if (currentUser == null) return;
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // 1. Username
        JLabel userLabel = new JLabel("👤 " + translations.get("Username")[0], SwingConstants.RIGHT);
        userLabel.setFont(getUnicodeFont(Font.BOLD, 16));
        JLabel userValue = new JLabel(currentUser.username);
        userValue.setFont(getUnicodeFont(Font.PLAIN, 16));
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.4; infoPanel.add(userLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.6; infoPanel.add(userValue, gbc);

        // 2. Email
        JLabel emailLabel = new JLabel("✉ " + translations.get("RegisteredEmail")[0], SwingConstants.RIGHT);
        emailLabel.setFont(getUnicodeFont(Font.BOLD, 16));
        JLabel emailValue = new JLabel(currentUser.email);
        emailValue.setFont(getUnicodeFont(Font.PLAIN, 16));
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.4; infoPanel.add(emailLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.6; infoPanel.add(emailValue, gbc);
        
        // Placeholder for more info
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.insets = new Insets(40, 10, 10, 10);
        infoPanel.add(new JLabel("--- Future features: Edit Profile, Address Management ---", SwingConstants.CENTER), gbc);
        
        infoPanel.revalidate();
        infoPanel.repaint();
    }
    
    // --- Generic Static Feature Panel (My Posts, WhatsApp, Stats) ---
    private JPanel createStaticFeaturePanel(String title) {
        JPanel panel = new GradientPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(getUnicodeFont(Font.BOLD, 24));
        panel.add(titleLabel, BorderLayout.NORTH);

        JLabel content = new JLabel("<html>This is the " + title + " feature page. <br>Future development will add data forms and tables here.</html>", SwingConstants.CENTER);
        content.setFont(getUnicodeFont(Font.PLAIN, 16));
        panel.add(content, BorderLayout.CENTER);

        panel.add(createBackButton("dashboard"), BorderLayout.SOUTH);

        return panel;
    }
    
    // --- Helper for Back Button (Used in all feature screens) ---
    private JPanel createBackButton(String cardName) {
        JButton backButton = styledButton("← Back to Dashboard", new Color(158,158,158));
        backButton.addActionListener(e -> cardLayout.show(mainPanel, cardName));
        
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        southPanel.setOpaque(false);
        southPanel.add(backButton);
        return southPanel;
    }
    
    // --- Core Logic for Cart/State Management ---
    private void addToCart(Product product) {
        // Check if item already exists
        for (CartItem item : cartList) {
            if (item.product.name.equals(product.name)) {
                item.quantity++;
                JOptionPane.showMessageDialog(this, "Added 1 more " + product.name + " to cart.", "Cart Update", JOptionPane.INFORMATION_MESSAGE);
                updateCartCount();
                return;
            }
        }
        
        // Add new item
        cartList.add(new CartItem(product, 1));
        JOptionPane.showMessageDialog(this, product.name + " added to cart.", "Cart Update", JOptionPane.INFORMATION_MESSAGE);
        updateCartCount();
    }
    
    private void updateCartCount() {
        if (cartCountLabel != null) {
            int count = cartList.stream().mapToInt(item -> item.quantity).sum();
            cartCountLabel.setText(String.valueOf(count));
        }
    }
    
    // --- Menu Setup (Keeping previous features) ---
    private void setupDashboardMenu(JButton profileBtn, JButton cartBtn) {
        JMenuBar menuBar = getJMenuBar();
        menuBar.removeAll(); 

        JMenu menu = new JMenu("☰ Menu");
        menu.setFont(getEmojiOrUnicodeFont(Font.PLAIN, 16));

        JMenuItem helpItem = new JMenuItem();
        JMenuItem settingsItem = new JMenuItem();
        JMenuItem logoutItem = new JMenuItem();

        Font menuItemFont = getUnicodeFont(Font.PLAIN, 14);
        helpItem.setFont(menuItemFont);
        settingsItem.setFont(menuItemFont);      
        helpItem.addActionListener(e -> JOptionPane.showMessageDialog(this, "Help: Contact support@farmerhub.com", "Help", JOptionPane.INFORMATION_MESSAGE));

        logoutItem.setFont(menuItemFont);

        helpItem.addActionListener(e -> JOptionPane.showMessageDialog(this, "Help: Contact support@farmerhub.com", "Help", JOptionPane.INFORMATION_MESSAGE));
        settingsItem.addActionListener(e -> JOptionPane.showMessageDialog(this, "Settings: No settings available yet.", "Settings", JOptionPane.INFORMATION_MESSAGE));
        logoutItem.addActionListener(e -> {
            currentUser = null;
            cartList.clear(); // Clear cart on logout
            updateCartCount();
            if (getJMenuBar() != null) getJMenuBar().setVisible(false);
            cardLayout.show(mainPanel, "login");
            if (loginUserField != null) loginUserField.setText("");
            if (loginPassField != null) loginPassField.setText("");
        });

        menu.add(helpItem);      
        menu.add(settingsItem);  
        menu.addSeparator();     
        menu.add(logoutItem);    
        
        menuBar.add(menu);
        menuBar.revalidate();
        menuBar.repaint();
    }


    // --- Language Update Method (Fix for the error is here) ---
    private void updateLanguage() {
        int langIndex = 0;
        if (langSelector != null) langIndex = Math.max(0, langSelector.getSelectedIndex());

        // Helper to update text based on name (for newly created elements)
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof JPanel) {
                for (Component child : ((JPanel) comp).getComponents()) {
                    if (child instanceof JLabel && child.getName() != null) {
                        String key = child.getName();
                        if (translations.containsKey(key)) {
                            ((JLabel) child).setText(translations.get(key)[langIndex]);
                        }
                    }
                }
            }
        }

        // Update the top app title
        if (appTitle != null) { appTitle.setText(translations.get("AppTitle")[langIndex]); }
        if (tagline != null) tagline.setText(translations.get("Tagline")[langIndex]);

        // Login labels & buttons 
        if (loginTitle != null) loginTitle.setText(translations.get("LoginTitle")[langIndex]);
        if (loginUserLabel != null) loginUserLabel.setText("👤 " + translations.get("Username")[langIndex]);
        if (loginPassLabel != null) loginPassLabel.setText("🔒 " + translations.get("Password")[langIndex]);
        if (loginBtn != null) loginBtn.setText(translations.get("LoginBtn")[langIndex]);
        if (signupBtn != null) signupBtn.setText(translations.get("SignupBtn")[langIndex]);
        if (forgotPassBtn != null) forgotPassBtn.setText(translations.get("ForgotPassBtn")[langIndex]); // NEW

        // Signup labels & buttons 
        if (signupTitle != null) signupTitle.setText(translations.get("SignupTitle")[langIndex]);
        if (signupUserLabel != null) signupUserLabel.setText("👤 " + translations.get("NewUser")[langIndex]);
        if (signupEmailLabel != null) signupEmailLabel.setText("✉ " + translations.get("Email")[langIndex]);
        if (signupPassLabel != null) signupPassLabel.setText("🔒 " + translations.get("NewPass")[langIndex]);
        if (signupConfirmPassLabel != null) signupConfirmPassLabel.setText("🔒 " + translations.get("ConfirmPass")[langIndex]);
        if (registerBtn != null) registerBtn.setText(translations.get("RegisterBtn")[langIndex]);
        if (backBtn != null) backBtn.setText(translations.get("BackBtn")[langIndex]);
        
        // Update Requirement labels 
        if (emailReqLabel != null) emailReqLabel.setText("<html><span style='font-size: 10px;'><b>" + translations.get("Email")[langIndex] + "</b>: " + translations.get("EmailReq")[langIndex] + "</span></html>");
        if (passReqLabel != null) passReqLabel.setText("<html><span style='font-size: 10px;'><b>" + translations.get("PassReq")[langIndex] + "</b>: " + translations.get("PassRule")[langIndex] + "</span></html>");

        // Dashboard labels & buttons
        if (dashTitle != null) dashTitle.setText(translations.get("DashboardTitle")[langIndex]);
        if (feature1Btn != null) feature1Btn.setText(translations.get("Feature1")[langIndex]);
        if (feature2Btn != null) feature2Btn.setText(translations.get("Feature2")[langIndex]);
        if (feature3Btn != null) feature3Btn.setText(translations.get("Feature3")[langIndex]);
        if (feature4Btn != null) feature4Btn.setText(translations.get("Feature4")[langIndex]);
        if (feature5Btn != null) feature5Btn.setText(translations.get("Feature5")[langIndex]);

        // --- Update Dashboard Buttons Text ---
        // Update Cart/Profile buttons text using robust component indexing (Dashboard is at index 2)
        try {
            JPanel dashboard = (JPanel) mainPanel.getComponent(2);
            // The header panel is the first component (BorderLayout.NORTH) of the dashboard.
            JPanel headerPanel = (JPanel) dashboard.getComponent(0);
            // The action button panel (containing profile/cart) is the second component (BorderLayout.EAST) of the header panel.
            JPanel actionBtnPanel = (JPanel) headerPanel.getComponent(1);

            // 1. Update Profile Button (index 0 of actionBtnPanel)
            JButton profileBtn = (JButton) actionBtnPanel.getComponent(0);
            profileBtn.setText(translations.get("ProfileBtn")[langIndex]);

            // 2. Update Cart Button (index 1 of actionBtnPanel is the wrapper panel)
            JPanel cartWrapper = (JPanel) actionBtnPanel.getComponent(1);
            JButton cartBtn = (JButton) cartWrapper.getComponent(0); // Cart button is the first (center) component of the wrapper
            cartBtn.setText(translations.get("CartBtn")[langIndex]);

        } catch (Exception e) {
            // This happens if updateLanguage is called before the dashboard panel is fully constructed.
            System.err.println("Non-critical error updating dashboard header buttons during translation: " + e.getMessage());
        }
        // --- End Dashboard Buttons Update ---
        
        // Update menu bar texts
        if (getJMenuBar() != null) {
            JMenuBar menuBar = getJMenuBar();
            if (menuBar.getMenuCount() > 0) {
                JMenu menu = menuBar.getMenu(0);
                if (menu != null) {
                    // Safely iterate menu components and update text for JMenuItem entries
                    for (Component comp : menu.getMenuComponents()) {
                        if (comp instanceof JMenuItem) {
                            JMenuItem mi = (JMenuItem) comp;
                            String action = mi.getText();
                            // Update by position or known action keywords
                            if (action == null || action.contains("Help") || action.contains("❓") ) {
                                mi.setText("❓ " + translations.get("Help")[langIndex]);
                            } else if (action.contains("Settings") || action.contains("⚙")) {
                                mi.setText("⚙ " + translations.get("Settings")[langIndex]);
                            } else if (action.contains("Logout") || action.contains("🚪")) {
                                mi.setText("🚪 " + translations.get("LogoutBtn")[langIndex]);
                            }
                        }
                    }
                }
            }
        }
        
        // Since cart and profile panels are updated dynamically, we need to call their update methods here
        updateCartPanel(); 
        updateProfilePanel();

        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        Font defaultFont = new Font("Nirmala UI", Font.PLAIN, 14);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("Button.font", defaultFont);
        UIManager.put("ComboBox.font", defaultFont);
        UIManager.put("MenuItem.font", defaultFont);
        UIManager.put("Menu.font", defaultFont);
        SwingUtilities.invokeLater(() -> new FarmerHubApp());
    }
}