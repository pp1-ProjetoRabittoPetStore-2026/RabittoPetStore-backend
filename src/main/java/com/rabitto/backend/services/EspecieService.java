package com.rabitto.backend.services;

import com.rabitto.backend.models.Especie;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EspecieService {

    private final Map<Especie, List<String>> racasPorEspecie = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        racasPorEspecie.put(Especie.CACHORRO, List.of(
                "SRD (Sem Raça Definida)", "Golden Retriever", "Labrador Retriever",
                "Pastor Alemão", "Buldogue Francês", "Shih Tzu",
                "Yorkshire Terrier", "Poodle", "Lulu da Pomerânia (Spitz)",
                "Husky Siberiano", "Border Collie", "Rottweiler",
                "Doberman", "Boxer", "Maltês",
                "Pug", "Chihuahua", "Beagle",
                "Dachshund (Teckel)", "Pit Bull", "Pastor Belga Malinois",
                "São Bernardo", "Bernese Mountain Dog", "Cocker Spaniel Inglês",
                "Cocker Spaniel Americano", "Schnauzer Miniatura", "Schnauzer Gigante",
                "Lhasa Apso", "Akita", "Chow Chow",
                "Shiba Inu", "Bichon Frisé", "Scottish Terrier",
                "West Highland White Terrier", "Bulldog Inglês", "Bull Terrier",
                "Weimaraner", "Dálmata", "Pastor Australiano",
                "Cavalier King Charles Spaniel", "Pequinês", "Boston Terrier",
                "Shar Pei", "Bloodhound", "Basset Hound",
                "Terra Nova", "Malamute do Alasca", "Galgo Inglês",
                "Whippet", "Cane Corso", "Fila Brasileiro",
                "Mastiff", "Bullmastiff", "Rhodesian Ridgeback",
                "Vizsla", "Pointer Inglês", "Setter Irlandês",
                "Collie", "Sheltie (Pastor de Shetland)", "Papillon",
                "Spitz Alemão", "Keeshond", "Samoieda",
                "Cão de Água Português", "Airedale Terrier", "Fox Terrier",
                "Jack Russell Terrier", "Staffordshire Bull Terrier", "Cão Cristado Chinês",
                "Basenji", "Borzoi", "Irish Wolfhound",
                "Deerhound Escocês", "Greyhound", "Komondor",
                "Puli", "Briard", "Beauceron",
                "Dogue Alemão", "Grande Danois", "Boerboel",
                "Cão de Gado Transmontano", "Rafeiro do Alentejo", "Cão da Serra da Estrela",
                "Cão de Castro Laboreiro", "Cão de Água Espanhol", "Bodeguero Andaluz",
                "Podengo Português", "Cão de Fila de São Miguel", "Barbado da Terceira",
                "Coton de Tuléar", "Havanese", "Lowchen",
                "Xoloitzcuintle", "Perro Sem Pelo do Peru", "Catahoula Leopard Dog",
                "Blue Lacy", "Treeing Walker Coonhound", "Bluetick Coonhound",
                "Redbone Coonhound", "Plott Hound", "American English Coonhound",
                "Otterhound", "Harrier", "Petit Basset Griffon Vendéen",
                "Grand Basset Griffon Vendéen", "Berger Picard", "Berger Blanc Suisse",
                "Pastor dos Pireneus", "Cão dos Pireneus", "Estrela Mountain Dog",
                "Entlebucher Mountain Dog", "Appenzeller", "Greater Swiss Mountain Dog",
                "Cimarron Uruguayo", "Ovelheiro Gaúcho", "Cão de Fila da Serra da Estrela"
        ));

        racasPorEspecie.put(Especie.GATO, List.of(
                "SRD (Sem Raça Definida)", "Persa", "Siamês",
                "Maine Coon", "Angorá", "Ragdoll",
                "Bengal", "Sphynx", "Scottish Fold",
                "British Shorthair", "American Shorthair", "Abissínio",
                "Oriental Shorthair", "Burmês", "Sagrado da Birmânia",
                "Siberiano", "Norueguês da Floresta", "Devon Rex",
                "Cornish Rex", "Exótico", "Himalaio",
                "Chartreux", "Azul Russo", "Bobtail Japonês",
                "Munchkin", "Savannah", "Ocicat",
                "Somali", "Tonquinês", "Turkish Van",
                "LaPerm", "American Curl", "Selkirk Rex",
                "Bombaim", "Korat", "Singapura",
                "Egyptian Mau", "Peterbald", "Balinês",
                "Manx", "Cymric", "Ragamuffin",
                "Burmilla", "Chausie", "Pixie-bob",
                "Sokoke", "Khao Manee", "Lykoi (Gato Lobo)",
                "American Wirehair", "California Spangled", "Ceylon",
                "Donskoy", "German Rex", "Havana Brown",
                "Highlander", "Kurilian Bobtail", "Mekong Bobtail",
                "Minskin", "Nebelung", "Raas",
                "Ojos Azules", "Serengeti", "Skookum",
                "Snowshoe", "Sokoto", "Thai",
                "Ural Rex", "York Chocolate", "Bambino",
                "Dwelf", "Elf Cat", "Kinkalow",
                "Lambkin", "Napoleon", "Scythian Rex",
                "Toyger"
        ));

        racasPorEspecie.put(Especie.PASSARO, List.of(
                "Calopsita", "Periquito Australiano", "Papagaio Verdadeiro",
                "Canário Belga", "Agapornis (Inseparável)", "Ring Neck (Periquito-de-colar)",
                "Cacatua", "Arara Canindé", "Arara Azul",
                "Diamante-de-Gould", "Mandarim", "Bicudo",
                "Curió", "Trinca-Ferro", "Sabiá-laranjeira",
                "Coleiro", "Cardeal", "Azulão",
                "Canário-da-terra", "Periquito-verde", "Lóris",
                "Mainá", "Jandaia", "Maritaca",
                "Tiriba", "Calafate", "Pintassilgo",
                "Pintagol", "Tico-tico", "Bem-te-vi",
                "Rouxinol", "Socozinho", "Garça",
                "Cisne", "Pavão", "Galinha-d'Angola",
                "Codorna", "Faisão", "Peru",
                "Pombo-correio", "Rolinha", "Caturrita",
                "Periquitão-maracanã", "Maitaca-verde", "Ararajuba",
                "Cacatua-de-crista-amarela", "Cacatua-rosa", "Calopsita-arlequim",
                "Periquito-inglês", "Periquito-holandês", "Agapornis-personata",
                "Agapornis-roseicollis", "Agapornis-fischeri", "Agapornis-nigrigenis",
                "Agapornis-lilianae", "Ring Neck-africano", "Ring Neck-indiano",
                "Canário-vermelho", "Canário-mosaico", "Canário-ágata",
                "Canário-isabela", "Canário-branco", "Diamante-mandarim",
                "Diamante-cauda-longa", "Diamante-citrino", "Diamante-orelha-laranja",
                "Capuchino", "Patativa", "Gaturamo",
                "Sanhaço", "Saíra", "Saí-azul",
                "Cabeçudo", "Tiziu", "Pardal",
                "Garça-branca", "Socó-boi", "Martim-pescador"
        ));

        racasPorEspecie.put(Especie.PEIXE, List.of(
                "Betta", "Goldfish (Quimbo)", "Guppy",
                "Molinésia", "Plati", "Espada",
                "Paulistinha (Zebra Danio)", "Neón (Cardinal Tetra)", "Acará Bandeira",
                "Ramirezi (Blue Ram)", "Apistograma", "Cascudo",
                "Coridora", "Otocinclus", "Limpa-vidro",
                "Barbus-de-Sumatra", "Barbus-cereja", "Gourami-anão",
                "Gourami-pérola", "Gourami-azul", "Gourami-beijador",
                "Oscar", "Ciclídeo Africano", "Acará Disco",
                "Kinguio-japonês", "Shubunkin", "Cometa",
                "Pleco-comum", "Pleco-leopardo", "Pleco-bristlenose",
                "Tetra-negro", "Matogrosso", "Rodóstomo",
                "Fantasma", "Killi", "Peixe-palhaço (marinho)",
                "Tang (Cirurgião-palhaço)", "Donzela", "Bodião-limpador",
                "Blênio", "Góbio", "Peixe-anjo-marinho",
                "Peixe-borboleta", "Foxface", "Peixe-porco",
                "Cavalo-marinho", "Baiacu-de-água-doce", "Arlequim (Harlequin Rasbora)",
                "Rasbora", "Danio", "White Cloud",
                "Corydoras-pygmaeus", "Corydoras-aeneus", "Corydoras-panda",
                "Corydoras-sterbai", "Corydoras-julii", "Corydoras-habrosus",
                "Tetra-congo", "Tetra-sangue", "Tetra-serpae",
                "Tetra-olho-vermelho", "Tetra-buenos-aires", "Tetra-vidro",
                "Lambari", "Piau", "Traíra",
                "Tilápia", "Carpa-comum", "Carpa-kói",
                "Carpa-capim", "Peixe-gato", "Bagre-africano",
                "Aruanã", "Silver Dollar", "Pacu",
                "Piranha", "Black Ghost Knifefish", "Elephantnose",
                "Leporinus", "Severum", "Geophagus",
                "Festivum", "Uaru", "Angelfish-altum",
                "Boesemani Rainbowfish", "Turquoise Rainbowfish", "Celebes Rainbowfish",
                "Halfbeak", "Guppy-endler", "Mickey Mouse Platy"
        ));

        racasPorEspecie.put(Especie.ROEDOR, List.of(
                "Hamster Sírio", "Hamster Anão Russo", "Hamster Chinês",
                "Hamster Roborovski", "Porquinho da Índia", "Porquinho da Índia Abissínio",
                "Porquinho da Índia Peruano", "Porquinho da Índia Skinny", "Porquinho da Índia Teddy",
                "Rato Twister", "Rato Dumbo", "Rato Rex",
                "Rato Husky", "Rato Esfumaçado", "Rato Albino",
                "Camundongo", "Gerbil da Mongólia", "Chinchila",
                "Degus", "Esquilo-da-Mongólia", "Preá",
                "Twister (Gerbilo)", "Hamster Branco de Inverno", "Hamster Campbell",
                "Porquinho da Índia Coronet", "Porquinho da Índia Silkie", "Porquinho da Índia Texel",
                "Porquinho da Índia Merino", "Porquinho da Índia Alpaca", "Porquinho da Índia Lunkarya",
                "Porquinho da Índia Sheba Mini Yak", "Rato-manchado", "Rato-rex-dumbo",
                "Rato-siamese", "Rato-berkshire", "Rato-hooded",
                "Rato-cape", "Rato-bareback", "Rato-satinate",
                "Camundongo-toyger", "Camundongo-satinate", "Camundongo-silver",
                "Chinchila Standard", "Chinchila Velvet", "Chinchila Ebony",
                "Chinchila Sapphire", "Chinchila Violet", "Chinchila Beige",
                "Chinchila Pastel", "Chinchila White Mosaic", "Chinchila Gold"
        ));

        racasPorEspecie.put(Especie.COELHO, List.of(
                "Coelho Anão", "Coelho Holandês (Dutch)", "Coelho Lionhead",
                "Coelho Angorá", "Coelho Rex", "Coelho Mini Rex",
                "Coelho Nova Zelândia", "Coelho Californiano", "Coelho Gigante de Flandres",
                "Coelho Havana", "Coelho Satin", "Coelho Holland Lop",
                "Coelho Mini Lop", "Coelho English Lop", "Coelho French Lop",
                "Coelho Fuzzy Lop", "Coelho Sussex", "Coelho Himalaio",
                "Coelho Chinchila", "Coelho Prateado", "Coelho Tan",
                "Coelho Beveren", "Coelho Arlequim", "Coelho Hotot",
                "Coelho Jersey Wooly", "Coelho Polonês", "Coelho American Fuzzy Lop",
                "Coelho English Spot", "Coelho Checkered Giant", "Coelho Belier",
                "Coelho Angorá Francês", "Coelho Angorá Inglês", "Coelho Angorá Gigante",
                "Coelho Angorá Satinizado", "Coelho Neozelandês Vermelho", "Coelho Neozelandês Preto",
                "Coelho Neozelandês Branco", "Coelho Californiano Preto", "Coelho Californiano Azul",
                "Coelho Chinchila Gigante", "Coelho Chinchila Americano", "Coelho Chinchila Standard",
                "Coelho Cabeça-de-leão", "Coelho Miniature Lion Lop", "Coelho Cashmere Lop",
                "Coelho Satinizado", "Coelho Satin Angorá", "Coelho Havana Azul",
                "Coelho Havana Preto", "Coelho Havana Chocolate", "Coelho Rex Castor",
                "Coelho Rex Chinchila", "Coelho Rex Lilás", "Coelho Rex Opal",
                "Coelho Rex Sable", "Coelho Mini Rex Chinchila", "Coelho Mini Rex Preto",
                "Coelho Mini Rex Azul", "Coelho Mini Rex Lilás", "Coelho Dutch Preto",
                "Coelho Dutch Azul", "Coelho Dutch Chinchila", "Coelho Dutch Tartaruga",
                "Coelho Dutch Tortoiseshell", "Coelho Dutch Steel", "Coelho Dutch Brabante"
        ));

        racasPorEspecie.put(Especie.REPTIL, List.of(
                "Tartaruga Tigre d'Água", "Jabuti Piranga", "Jabuti Tinga",
                "Cágado", "Iguana Verde", "Lagarto Teiú",
                "Dragão Barbudo (Bearded Dragon)", "Gecko Leopardo", "Gecko Tokay",
                "Lagartixa", "Camaleão Velado", "Camaleão Pantera",
                "Anolis-verde", "Anolis-marrom", "Skink-de-língua-azul",
                "Skink-de-fogo", "Cobra-do-milho", "Cobra-real (Ball Python)",
                "Jiboia-arco-íris", "Jiboia-constritora", "Falsa-coral",
                "Cobra-do-leite (Milk Snake)", "Kingsnake", "Python-verde (Green Tree Python)",
                "Tartaruga-de-orelha-vermelha", "Tartaruga-pintada", "Tartaruga-de-caixa",
                "Tartaruga-mordedora (Snapping Turtle)", "Matamatá", "Tartaruga-grega (Mediterrânea)",
                "Tartaruga-russa (Horsfield)", "Tartaruga-leopardo", "Tartaruga-sulcata",
                "Tartaruga-de-esporas", "Gecko-diurno-gigante-de-Madagascar", "Gecko-de-crista",
                "Gecko-gargoyle", "Lagartixa-doméstica-tropical", "Salamandra-de-fogo",
                "Axolote", "Tritão", "Rã-arborícola-verde",
                "Rã-de-olhos-vermelhos", "Perereca-de-waxy-monkey", "Sapo-cururu",
                "Sapo-parteiro", "Dragon d'água-chinês", "Basilisco-verde",
                "Teiú-argentino (Overo)", "Teiú-preto-e-branco", "Monitord'água (Water Monitor)",
                "Cágado-pescoço-de-cobra", "Cágado-de-ouvido-amarelo", "Hingeback-tortoise",
                "Tartaruga-marginal", "Tartaruga-de-hermann", "Tartaruga-dos-balcãs",
                "Tortoise-de-rabié", "Sucuri (licenciamento)", "Cascavel (licenciamento)",
                "Jararaca (licenciamento)", "Urutu (licenciamento)"
        ));

        racasPorEspecie.put(Especie.FURAO, List.of(
                "Furão Sable", "Furão Albino", "Furão Chocolate",
                "Furão Canela (Cinnamon)", "Furão Preto (Black Sable)", "Furão Prateado (Silver)",
                "Furão Panda", "Furão Mitt", "Furão Blaze",
                "Furão Angorá", "Furão Champanhe", "Furão Chocolate Mitt",
                "Furão Sable Mitt", "Furão Silver Mitt", "Furão Panda Mitt",
                "Furão Blaze Mitt", "Furão Point", "Furão Roan",
                "Furão Solid", "Furão White-footed", "Furão Marked-white",
                "Furão Standard Sable", "Furão Dark-eyed White", "Furão Black-eyed White",
                "Furão Sable Point", "Furão Siamese", "Furão Silver Point",
                "Furão Patterned"
        ));

        racasPorEspecie.put(Especie.OURICO, List.of(
                "Ouriço Pigmeu Africano", "Ouriço Europeu", "Ouriço Egípcio (Orelhudo)",
                "Ouriço-de-barriga-branca", "Ouriço-argelino", "Ouriço-somali",
                "Ouriço-sul-africano", "Ouriço-da-Ásia-Menor", "Ouriço-indiano-de-cauda-longa",
                "Ouriço-do-deserto", "Ouriço-de-Gobi", "Ouriço-da-manchúria",
                "Ouriço-pigmeu-argelino"
        ));

        racasPorEspecie.put(Especie.MINI_PIG, List.of(
                "Mini Pig", "Micro Pig", "Teacup Pig",
                "Kunekune", "Porco Vietnamita (Pot-bellied)", "Juliana",
                "Chinese Pot-bellied", "Mini Maialino", "Gottingen Mini Pig",
                "Yucatan Mini Pig", "Panepinto Micro Pig", "American Mini Pig",
                "Moscow Mini Pig", "Mangalitsa Mini Pig", "Miniature Hereford Pig",
                "African Pygmy Pig", "Ossabaw Island Hog", "Woburn Micro Pig",
                "Royal Dandie Mini Pig", "Buttercup Mini Pig", "KuneKune Branco",
                "KuneKune Preto", "KuneKune Tigrado", "KuneKune Gengibre"
        ));
    }

    public Map<Especie, List<String>> getRacasPorEspecie() {
        return Collections.unmodifiableMap(racasPorEspecie);
    }

    public List<String> getRacas(Especie especie) {
        return racasPorEspecie.getOrDefault(especie, List.of());
    }
}
