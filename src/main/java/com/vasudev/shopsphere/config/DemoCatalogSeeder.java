package com.vasudev.shopsphere.config;

import com.vasudev.shopsphere.entity.Category;
import com.vasudev.shopsphere.entity.Product;
import com.vasudev.shopsphere.repository.CategoryRepository;
import com.vasudev.shopsphere.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DemoCatalogSeeder seeds initial categories and products for local and demo environments.
 * It is strictly restricted to non-production profiles ('local', 'dev', 'demo').
 * The seeder is idempotent: it will not duplicate categories or products if executed repeatedly.
 */
@Component
@Profile({"local", "dev", "demo"})
public class DemoCatalogSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoCatalogSeeder.class);

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public DemoCatalogSeeder(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Starting ShopSphere demo catalog seeding...");
        seedCatalog();
        log.info("ShopSphere demo catalog seeding completed successfully.");
    }

    @Transactional
    public CatalogSeedResult seedCatalog() {
        int categoriesCreated = 0;
        int productsCreated = 0;

        // 1. Define Category definitions
        List<CategorySeedData> categorySeedList = getCategorySeedData();

        // 2. Fetch all existing products once for fast, idempotent matching
        Map<String, Product> existingProductsByName = productRepository.findAll().stream()
                .collect(Collectors.toMap(
                        p -> p.getName().trim().toLowerCase(),
                        p -> p,
                        (existing, replacement) -> existing
                ));

        for (CategorySeedData catData : categorySeedList) {
            // Find or create Category
            Optional<Category> existingCategoryOpt = categoryRepository.findByName(catData.name);
            Category category;
            if (existingCategoryOpt.isPresent()) {
                category = existingCategoryOpt.get();
                log.debug("Category '{}' already exists with ID {}", category.getName(), category.getId());
            } else {
                category = new Category(catData.name, catData.description);
                category.setActive(true);
                category = categoryRepository.save(category);
                categoriesCreated++;
                log.info("Created category '{}' (ID: {})", category.getName(), category.getId());
            }

            // Seed Products for this category
            for (ProductSeedData prodData : catData.products) {
                String normalizedName = prodData.name.trim().toLowerCase();
                if (existingProductsByName.containsKey(normalizedName)) {
                    Product existingProduct = existingProductsByName.get(normalizedName);
                    if (existingProduct != null) {
                        if (existingProduct.getImageUrl() == null || existingProduct.getImageUrl().isBlank() || !existingProduct.getImageUrl().equals(prodData.imageUrl)) {
                            existingProduct.setImageUrl(prodData.imageUrl);
                            productRepository.save(existingProduct);
                            log.info("Updated imageUrl for demo product '{}' (ID: {})", existingProduct.getName(), existingProduct.getId());
                        } else {
                            log.debug("Product '{}' already exists and is up to date, skipping.", prodData.name);
                        }
                    }
                    continue;
                }

                Product product = new Product(
                        prodData.name,
                        prodData.description,
                        prodData.price,
                        prodData.stockQuantity,
                        true,
                        category,
                        prodData.imageUrl
                );
                Product savedProduct = productRepository.save(product);
                existingProductsByName.put(normalizedName, savedProduct);
                productsCreated++;
                log.info("Created product '{}' in category '{}' (ID: {})", savedProduct.getName(), category.getName(), savedProduct.getId());
            }
        }

        log.info("Seeding summary: {} categories inserted, {} products inserted.", categoriesCreated, productsCreated);
        return new CatalogSeedResult(categoriesCreated, productsCreated);
    }

    public static class CatalogSeedResult {
        private final int categoriesCreated;
        private final int productsCreated;

        public CatalogSeedResult(int categoriesCreated, int productsCreated) {
            this.categoriesCreated = categoriesCreated;
            this.productsCreated = productsCreated;
        }

        public int getCategoriesCreated() {
            return categoriesCreated;
        }

        public int getProductsCreated() {
            return productsCreated;
        }
    }

    static class CategorySeedData {
        final String name;
        final String description;
        final List<ProductSeedData> products;

        CategorySeedData(String name, String description, List<ProductSeedData> products) {
            this.name = name;
            this.description = description;
            this.products = products;
        }
    }

    static class ProductSeedData {
        final String name;
        final String description;
        final BigDecimal price;
        final Integer stockQuantity;
        final String imageUrl;

        ProductSeedData(String name, String description, BigDecimal price, Integer stockQuantity, String imageUrl) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.stockQuantity = stockQuantity;
            this.imageUrl = imageUrl;
        }
    }

    List<CategorySeedData> getCategorySeedData() {
        List<CategorySeedData> list = new ArrayList<>();

        // 1. Electronics & Gadgets
        list.add(new CategorySeedData(
                "Electronics & Gadgets",
                "High-performance audio, smart accessories, charging tech, and computing peripherals.",
                List.of(
                        new ProductSeedData(
                                "ShopSphere Pro Wireless Noise-Cancelling Headphones",
                                "Premium over-ear wireless headphones featuring active noise cancellation, 40mm dynamic drivers, 35-hour battery life, and ultra-soft memory foam earcups.",
                                new BigDecimal("149.99"),
                                45,
                                "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere UltraFast 65W GaN USB-C Charger",
                                "Compact Gallium Nitride (GaN) wall charger with 3 ports (2x USB-C, 1x USB-A) supporting Power Delivery 3.0 fast charging for laptops, tablets, and smartphones.",
                                new BigDecimal("29.99"),
                                120,
                                "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere SoundPulse Portable Bluetooth Speaker",
                                "Rugged IPX7 waterproof portable speaker delivering 360-degree stereo sound, punchy bass, built-in speakerphone mic, and up to 20 hours of continuous playback.",
                                new BigDecimal("59.99"),
                                60,
                                "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Ergonomic Multi-Device Wireless Mouse",
                                "Contoured wireless mouse with silent clicks, customizable side buttons, adjustable DPI up to 4000, and seamless multi-host Bluetooth and 2.4GHz switching.",
                                new BigDecimal("34.99"),
                                85,
                                "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere 4K Ultra-HD Webcam with Dual Mics",
                                "Crystal-clear 4K UHD autofocus streaming webcam with dual noise-reduction microphones, built-in privacy shutter, and universal monitor mount.",
                                new BigDecimal("69.99"),
                                40,
                                "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?auto=format&fit=crop&w=600&q=80"
                        )
                )
        ));

        // 2. Home & Kitchen
        list.add(new CategorySeedData(
                "Home & Kitchen",
                "Modern cookware, coffee equipment, digital appliances, and airtight food storage essentials.",
                List.of(
                        new ProductSeedData(
                                "ShopSphere Stainless Steel Electric Gooseneck Kettle",
                                "Precision pour-over electric kettle with variable temperature control, 1-hour keep-warm function, LCD display, and rapid boiling.",
                                new BigDecimal("49.99"),
                                50,
                                "https://images.unsplash.com/photo-1570968915860-54d5c301fa9f?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Precision Digital Kitchen Scale",
                                "Ultra-accurate digital food scale with 0.1g resolution, touch tare function, backlit LCD display, and durable stainless steel weighing platform.",
                                new BigDecimal("19.99"),
                                100,
                                "https://images.unsplash.com/photo-1594911772125-07fc7a2d8d9f?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere 10-Piece Ceramic Non-Stick Cookware Set",
                                "Eco-friendly non-toxic ceramic non-stick pots and pans set compatible with all stovetops including induction, complete with tempered glass lids.",
                                new BigDecimal("129.99"),
                                30,
                                "https://images.unsplash.com/photo-1584269600464-37b1b58a9fe7?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Airtight Food Storage Container Set",
                                "7-piece modular BPA-free food container set with silicone airtight locking lids, stackable space-saving design, and chalkboard labels.",
                                new BigDecimal("39.99"),
                                75,
                                "https://images.unsplash.com/photo-1610557892470-55d9e80c0bce?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Premium French Press Coffee Maker",
                                "Heat-resistant borosilicate glass coffee press with 4-level stainless steel filtration system and ergonomic cool-touch handle (34 oz).",
                                new BigDecimal("27.99"),
                                65,
                                "https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=600&q=80"
                        )
                )
        ));

        // 3. Apparel & Fashion
        list.add(new CategorySeedData(
                "Apparel & Fashion",
                "Everyday sustainable essentials, premium cotton basics, outerwear, and comfort-stretch apparel.",
                List.of(
                        new ProductSeedData(
                                "ShopSphere Classic Organic Cotton Crewneck T-Shirt",
                                "Pre-shrunk 100% certified organic combed ring-spun cotton t-shirt with reinforced ribbed collar and breathable all-day comfort.",
                                new BigDecimal("24.99"),
                                150,
                                "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere All-Weather Windbreaker Jacket",
                                "Water-repellent, windproof lightweight zip-up jacket with adjustable hood, breathable mesh lining, and packable travel pouch.",
                                new BigDecimal("79.99"),
                                55,
                                "https://images.unsplash.com/photo-1548883354-7622d03aca27?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Comfort-Stretch Denim Jeans",
                                "Modern straight-fit five-pocket denim jeans crafted from durable ring-spun cotton with engineered stretch for flexibility and durability.",
                                new BigDecimal("54.99"),
                                80,
                                "https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Merino Wool Everyday Socks (3-Pack)",
                                "Naturally moisture-wicking and odor-resistant fine Australian Merino wool crew socks with reinforced heel and seamless toe cushioning.",
                                new BigDecimal("18.99"),
                                110,
                                "https://images.unsplash.com/photo-1582966772680-860e372bb558?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Lightweight Athletic Running Hoodie",
                                "Moisture-wicking four-way stretch pullover athletic hoodie featuring thumbhole cuffs, kangaroo pocket, and reflective safety accents.",
                                new BigDecimal("44.99"),
                                70,
                                "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?auto=format&fit=crop&w=600&q=80"
                        )
                )
        ));

        // 4. Books & Stationery
        list.add(new CategorySeedData(
                "Books & Stationery",
                "Productivity journals, premium fountain pens, fine-liner art pens, and minimalist desk organizers.",
                List.of(
                        new ProductSeedData(
                                "ShopSphere Hardcover Dotted Journal Notebook",
                                "160 GSM bleed-resistant bamboo paper hardcover dot grid journal with expandable inner pocket, dual ribbon bookmarks, and elastic band closure.",
                                new BigDecimal("15.99"),
                                90,
                                "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Executive Fountain Pen with Ink Converter",
                                "Handcrafted metallic matte black fountain pen with stainless steel fine nib, smooth ink flow feed, and universal piston refill converter.",
                                new BigDecimal("28.99"),
                                45,
                                "https://images.unsplash.com/photo-1583485088034-697b5bc54ccd?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Minimalist Metal Desk Organizer",
                                "Sleek powder-coated steel 6-compartment desk organizer with dedicated pen holder, sticky note tray, and sliding drawer for office supplies.",
                                new BigDecimal("22.99"),
                                60,
                                "https://images.unsplash.com/photo-1585776245991-cf89dd7fc73a?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Archival Fine-Liner Drawing Pens (8-Pack)",
                                "Waterproof, fade-resistant black pigment ink fine-liner pen set with micro-point tip sizes ranging from 0.05mm to 1.0mm and brush tip.",
                                new BigDecimal("16.99"),
                                80,
                                "https://images.unsplash.com/photo-1586075010923-2dd4570fb338?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Weekly Productivity Planner Pad",
                                "Undated 60-sheet tear-off desk planner featuring habit tracking, daily priority matrix, weekly schedule overview, and goal setting sections.",
                                new BigDecimal("12.99"),
                                100,
                                "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?auto=format&fit=crop&w=600&q=80"
                        )
                )
        ));

        // 5. Fitness & Outdoors
        list.add(new CategorySeedData(
                "Fitness & Outdoors",
                "High-durability workout gear, resistance sets, thermal bottles, and lightweight camping backpacks.",
                List.of(
                        new ProductSeedData(
                                "ShopSphere Non-Slip High-Density Yoga Mat",
                                "Extra thick 6mm eco-friendly TPE exercise yoga mat with laser-engraved alignment guides, non-slip textured grip, and carrying strap.",
                                new BigDecimal("32.99"),
                                65,
                                "https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Insulated Stainless Steel Water Bottle (32oz)",
                                "Double-wall vacuum-insulated 18/8 food-grade stainless steel bottle keeping drinks icy cold for 24 hours or piping hot for 12 hours.",
                                new BigDecimal("24.99"),
                                130,
                                "https://images.unsplash.com/photo-1602143407151-7111542de6e8?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Adjustable Resistance Bands Set (5-Level)",
                                "Stackable natural latex resistance tube bands (10-50 lbs) with cushioned handles, sturdy ankle straps, and heavy-duty door anchor.",
                                new BigDecimal("21.99"),
                                95,
                                "https://images.unsplash.com/photo-1598289431512-b97b0917affc?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Lightweight Waterproof Hiking Backpack 30L",
                                "Tear-resistant nylon trail backpack with breathable ergonomic shoulder straps, hydration bladder sleeve, rain cover, and trekking pole loops.",
                                new BigDecimal("64.99"),
                                40,
                                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Speed Jump Rope with Ball Bearings",
                                "Fully adjustable tangle-free steel wire jump rope with 360-degree dual ball bearings and aluminum anti-slip knurled handles for cardio training.",
                                new BigDecimal("14.99"),
                                120,
                                "https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=600&q=80"
                        )
                )
        ));

        // 6. Beauty & Personal Care
        list.add(new CategorySeedData(
                "Beauty & Personal Care",
                "Dermatologist-tested skincare serums, moisturizers, mineral SPF protection, and nourishing oils.",
                List.of(
                        new ProductSeedData(
                                "ShopSphere Vitamin C Brightening Facial Serum",
                                "Advanced antioxidant facial serum formulated with 15% pure L-Ascorbic acid, Ferulic acid, and Vitamin E to brighten tone and smooth texture.",
                                new BigDecimal("22.99"),
                                75,
                                "https://images.unsplash.com/photo-1620916566398-39f1143ab7be?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Hydrating Hyaluronic Daily Moisturizer",
                                "Lightweight oil-free gel moisturizer enriched with multi-molecular hyaluronic acid, niacinamide, and soothing ceramide complex.",
                                new BigDecimal("19.99"),
                                85,
                                "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Gentle Exfoliating Face Cleanser",
                                "Non-stripping balancing daily gel face wash with micro-buffing AHA/BHA blend, aloe vera, and green tea leaf extract for all skin types.",
                                new BigDecimal("16.99"),
                                90,
                                "https://images.unsplash.com/photo-1556228720-195a672e8a03?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Organic Moroccan Argan Hair Oil",
                                "Cold-pressed 100% pure organic argan oil treatment for hair nourishment, frizz control, heat protection, and natural shine.",
                                new BigDecimal("24.99"),
                                60,
                                "https://images.unsplash.com/photo-1601049541289-9b1b7bbbfe19?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Mineral UV Defense Sunscreen SPF 50",
                                "Broad-spectrum SPF 50 non-greasy mineral sunscreen with sheer zinc oxide, antioxidant botanicals, and water resistance up to 80 minutes.",
                                new BigDecimal("21.99"),
                                110,
                                "https://images.unsplash.com/photo-1598440947619-2c35fc9aa908?auto=format&fit=crop&w=600&q=80"
                        )
                )
        ));

        // 7. Smart Home & Lighting
        list.add(new CategorySeedData(
                "Smart Home & Lighting",
                "Intelligent LED ambiance, architect task lamps, smart plugs, and ultrasonic aromatherapy.",
                List.of(
                        new ProductSeedData(
                                "ShopSphere Smart LED RGB Ambient Light Bar",
                                "Dynamic RGB color-changing light bar with music synchronization, 16 million colors, customizable lighting modes, and app remote control.",
                                new BigDecimal("39.99"),
                                50,
                                "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Dimmable LED Architect Desk Lamp",
                                "Multi-angle adjustable swing arm metal desk lamp with 5 color temperatures, step-less touch dimming, and integrated USB charging port.",
                                new BigDecimal("45.99"),
                                45,
                                "https://images.unsplash.com/photo-1534353436294-0dbd4bdac845?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Wi-Fi Smart Plug with Energy Monitoring (4-Pack)",
                                "Compact smart socket plugs with real-time power usage monitoring, timer scheduling, and voice assistant compatibility without a hub.",
                                new BigDecimal("34.99"),
                                70,
                                "https://images.unsplash.com/photo-1558002038-1055907df827?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Motion-Sensor LED Night Light (2-Pack)",
                                "Warm white automatic dusk-to-dawn infrared motion sensing night lights with dual brightness modes and plug-in space-saving design.",
                                new BigDecimal("17.99"),
                                95,
                                "https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Ultrasonic Cool Mist Aromatherapy Diffuser",
                                "500ml quiet essential oil diffuser with 7 ambient mood lights, 4 timer settings, automatic waterless shut-off, and mist control.",
                                new BigDecimal("29.99"),
                                60,
                                "https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?auto=format&fit=crop&w=600&q=80"
                        )
                )
        ));

        // 8. Office & Workspace
        list.add(new CategorySeedData(
                "Office & Workspace",
                "Ergonomic memory foam cushions, laptop risers, desk pads, and cable management accessories.",
                List.of(
                        new ProductSeedData(
                                "ShopSphere Memory Foam Ergonomic Seat Cushion",
                                "Orthopedic coccyx relief seat cushion made of 100% high-density memory foam with breathable non-slip washable velour cover.",
                                new BigDecimal("36.99"),
                                55,
                                "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Aluminum Adjustable Laptop Stand",
                                "Foldable ergonomic aluminum laptop riser with 6-level angle adjustments, heat-dissipating ventilation, and non-slip silicone pads.",
                                new BigDecimal("29.99"),
                                80,
                                "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Dual-Sided Eco-Leather Desk Pad Mat",
                                "Extra large 36\" x 17\" waterproof and scratch-resistant PU leather mouse pad desk protector with smooth gliding surface.",
                                new BigDecimal("21.99"),
                                100,
                                "https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Cable Management Box & Cord Clips Kit",
                                "Set of 2 heavy-duty heat-resistant cable organizer boxes with silicone cord holders and reusable hook-and-loop cable ties.",
                                new BigDecimal("18.99"),
                                110,
                                "https://images.unsplash.com/photo-1581291518857-4e27b48ff24e?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Under-Desk Footrest with Massage Texture",
                                "Ergonomic angled footrest cushion with textured acupressure surface and removable high-density extension piece.",
                                new BigDecimal("32.99"),
                                40,
                                "https://images.unsplash.com/photo-1592078615290-033ee584e267?auto=format&fit=crop&w=600&q=80"
                        )
                )
        ));

        // 9. Toys, Games & Hobbies
        list.add(new CategorySeedData(
                "Toys, Games & Hobbies",
                "Panoramic jigsaw puzzles, wooden strategy games, deluxe acrylic art supplies, and brain teasers.",
                List.of(
                        new ProductSeedData(
                                "ShopSphere 1000-Piece Panoramic Landscape Puzzle",
                                "Precision-cut recycled cardboard puzzle with vibrant anti-glare matte finish, full-size reference poster, and zero puzzle dust.",
                                new BigDecimal("19.99"),
                                50,
                                "https://images.unsplash.com/photo-1587731556938-38755b4803a6?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Premium Wooden Chess & Checkers Set",
                                "Handcrafted 15-inch folding magnetic walnut board with carved wooden Staunton chess pieces and felted interior storage slots.",
                                new BigDecimal("38.99"),
                                35,
                                "https://images.unsplash.com/photo-1529699211952-734e80c4d42b?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Strategy Board Game - Realm of Wonders",
                                "Engaging tactical kingdom-building board game for 2-4 players with custom miniatures, modular terrain tiles, and 60-minute playtime.",
                                new BigDecimal("49.99"),
                                40,
                                "https://images.unsplash.com/photo-1610890716171-6b1bb98ffd09?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Deluxe Acrylic Paint Set (24 Colors)",
                                "High-pigment professional non-toxic acrylic paint tubes with 6 nylon artist brushes, mixing palette, and storage case.",
                                new BigDecimal("26.99"),
                                65,
                                "https://images.unsplash.com/photo-1513364776144-60967b0f800f?auto=format&fit=crop&w=600&q=80"
                        ),
                        new ProductSeedData(
                                "ShopSphere Handcrafted Wooden Brain Teaser Puzzle",
                                "Challenging 3D geometric interlocking wooden mechanical puzzle crafted from sustainable solid wood for teens and adults.",
                                new BigDecimal("14.99"),
                                75,
                                "https://images.unsplash.com/photo-1596461404969-9ae70f2830c1?auto=format&fit=crop&w=600&q=80"
                        )
                )
        ));

        return list;
    }
}
