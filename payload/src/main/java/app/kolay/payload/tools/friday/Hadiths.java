package app.kolay.payload.tools.friday;

/**
 * Well-known hadiths (Turkish meaning, not a word-for-word translation) from Sahih al-Bukhari, Sahih Muslim and
 * Jami' at-Tirmidhi. Each row is {text, source}. A number is given only where it is known with confidence; otherwise
 * just the collection. To add one: append a row (keep it under ~230 characters so it stays readable on the image),
 * check it against the printed collection first, and never invent a number.
 */
final class Hadiths {
    private Hadiths() {}

    static final String[][] ALL = {
        // --- Cuma (Friday) ---
        {"Güneşin doğduğu günlerin en hayırlısı cuma günüdür. Âdem o gün yaratıldı, o gün cennete girdi.", "Müslim 854"},
        {"Cuma günü öyle bir saat vardır ki, bir Müslüman kul o saate rastlayıp Allah'tan hayır dilerse, Allah onu mutlaka verir.", "Buhârî 935"},
        {"Beş vakit namaz, cumadan cumaya ve ramazandan ramazana, büyük günahlardan kaçınıldığı sürece aralarındaki günahlara kefarettir.", "Müslim 233"},
        {"Kim cuma günü güzelce yıkanır, erkenden camiye gider, imama yakın oturup hutbeyi dinler ve boş işlerle uğraşmazsa, attığı her adım için bir yıllık oruç ve namaz sevabı alır.", "Tirmizî 496"},
        {"Biz dünyaya en son gelen ümmetiz; ama kıyamet gününde en öndeyiz.", "Buhârî ve Müslim"},

        // --- Niyet, ihlas, ameller ---
        {"Ameller niyetlere göredir. Herkes yalnızca niyet ettiğinin karşılığını alır.", "Buhârî 1 · Müslim"},
        {"Allah sizin suretlerinize ve mallarınıza bakmaz; kalplerinize ve amellerinize bakar.", "Müslim 2564"},
        {"Amellerin Allah'a en sevimli olanı, az da olsa devamlı olanıdır.", "Buhârî ve Müslim"},
        {"Din nasihattir.", "Müslim 55"},
        {"Kendisini ilgilendirmeyen şeyleri bırakması, bir kişinin Müslümanlığının güzelliğindendir.", "Tirmizî 2317"},
        {"Sana şüphe veren şeyi bırak, şüphe vermeyene bak.", "Tirmizî 2518"},

        // --- İman ve kardeşlik ---
        {"Hiçbiriniz, kendisi için istediğini kardeşi için de istemedikçe gerçek anlamda iman etmiş olmaz.", "Buhârî 13"},
        {"Müslüman, dilinden ve elinden diğer Müslümanların güvende olduğu kimsedir.", "Buhârî 10"},
        {"Müslüman Müslümanın kardeşidir; ona zulmetmez ve onu düşmanına teslim etmez. Kim kardeşinin ihtiyacını giderirse, Allah da onun ihtiyacını giderir.", "Buhârî ve Müslim"},
        {"Birbirinize haset etmeyin, birbirinize buğzetmeyin, birbirinize sırt çevirmeyin. Ey Allah'ın kulları, kardeş olun.", "Buhârî ve Müslim"},
        {"İman etmedikçe cennete giremezsiniz; birbirinizi sevmedikçe de iman etmiş olmazsınız. Size, yaptığınızda birbirinizi seveceğiniz bir şey söyleyeyim mi? Aranızda selamı yayın.", "Müslim 54"},
        {"Kim bir Müslümanın dünya sıkıntılarından birini giderirse, Allah da onun kıyamet günü sıkıntılarından birini giderir. Kim bir Müslümanın ayıbını örterse, Allah da dünyada ve ahirette onun ayıbını örter.", "Müslim 2699"},
        {"Kim bizi aldatırsa bizden değildir.", "Müslim"},
        {"Hayâ imandandır.", "Buhârî ve Müslim"},
        {"Temizlik imanın yarısıdır.", "Müslim 223"},

        // --- Ahlak ---
        {"Sizin en hayırlınız, ahlâkı en güzel olanınızdır.", "Buhârî ve Müslim"},
        {"Mizanda güzel ahlâktan daha ağır gelecek bir şey yoktur.", "Tirmizî 2002"},
        {"Kolaylaştırın, zorlaştırmayın; müjdeleyin, nefret ettirmeyin.", "Buhârî ve Müslim"},
        {"Güçlü kimse güreşte rakibini yenen değildir. Asıl güçlü, öfkelendiği zaman kendine hâkim olandır.", "Buhârî 6114"},
        {"Doğruluk iyiliğe, iyilik de cennete götürür. Yalan ise kötülüğe, kötülük de cehenneme götürür.", "Buhârî ve Müslim"},
        {"Münafığın alameti üçtür: konuştuğunda yalan söyler, söz verdiğinde sözünden döner, kendisine emanet verildiğinde hıyanet eder.", "Buhârî ve Müslim"},
        {"Allah güzeldir, güzeli sever.", "Müslim"},
        {"Müminin durumu ne şaşırtıcıdır! Her işi kendisi için hayırdır. Sevinirse şükreder, bu onun için hayır olur; bir sıkıntıya uğrarsa sabreder, bu da onun için hayır olur.", "Müslim"},
        {"Sabır, musibetin ilk anında gösterilendir.", "Buhârî ve Müslim"},
        {"Kim Allah için tevazu gösterirse, Allah onu yükseltir.", "Müslim"},
        {"Allah ve ahiret gününe inanan kimse, ya hayır söylesin ya da sussun.", "Buhârî ve Müslim"},
        {"İnsanlara teşekkür etmeyen, Allah'a da şükretmiş olmaz.", "Tirmizî 1954"},
        {"Kuvvetli mümin, Allah katında zayıf müminden daha hayırlı ve daha sevimlidir; ikisinde de hayır vardır.", "Müslim"},

        // --- Merhamet, komşu, aile ---
        {"Merhamet etmeyene merhamet olunmaz.", "Buhârî ve Müslim"},
        {"Merhamet edenlere Rahmân merhamet eder. Yeryüzündekilere merhamet edin ki gökteki de size merhamet etsin.", "Tirmizî 1924"},
        {"Cebrail bana komşuyu o kadar çok tavsiye etti ki, onu mirasçı kılacak sandım.", "Buhârî ve Müslim"},
        {"Allah'a ve ahiret gününe inanan, komşusuna eziyet etmesin; misafirine ikram etsin.", "Buhârî ve Müslim"},
        {"Bir adam sordu: İyi davranmama en çok kim hak sahibidir? Peygamber şöyle dedi: Annen. Sonra kim? Annen. Sonra kim? Annen. Sonra kim? Babandır.", "Buhârî ve Müslim"},
        {"Akrabalık bağını kesen cennete giremez.", "Buhârî ve Müslim"},

        // --- Sadaka, hayır, ilim ---
        {"Tebessüm etmen, kardeşin için sadakadır.", "Tirmizî 1956"},
        {"Güzel söz sadakadır.", "Buhârî ve Müslim"},
        {"Her iyilik sadakadır.", "Buhârî ve Müslim"},
        {"Sadaka malı eksiltmez.", "Müslim"},
        {"Bir Müslüman bir ağaç diker veya bir ekin ekerse, ondan bir insan, kuş ya da hayvan yediğinde bu onun için sadaka olur.", "Buhârî ve Müslim"},
        {"Kim bir hayra vesile olursa, onu yapan kimsenin sevabı kadar sevap alır.", "Müslim"},
        {"İnsan ölünce ameli kesilir; ancak üç şey hariç: sürekli sadaka, faydalı ilim ve kendisine dua eden hayırlı evlat.", "Müslim 1631"},
        {"Sizin en hayırlınız, Kur'an'ı öğrenen ve öğretendir.", "Buhârî"},
        {"Kim ilim öğrenmek için bir yola girerse, Allah ona cennete giden yolu kolaylaştırır.", "Müslim 2699"},

        // --- Zikir ve dua ---
        {"Dile hafif, mizanda ağır, Rahmân'a sevimli iki söz vardır: Sübhânallâhi ve bihamdih, Sübhânallâhil-azîm.", "Buhârî ve Müslim"},
        {"Kim günde yüz defa «Sübhânallâhi ve bihamdih» derse, günahları denizin köpüğü kadar olsa bile bağışlanır.", "Buhârî 6405"},
        {"Rabbini zikreden ile zikretmeyenin durumu, diri ile ölünün durumu gibidir.", "Buhârî"},
        {"Dua ibadetin ta kendisidir.", "Tirmizî 3372"},
        {"Ey kalpleri evirip çeviren Allah'ım, kalbimi dinin üzerinde sabit kıl.", "Tirmizî 2140"},

        // --- Dünya, nimet, tevekkül ---
        {"İki nimet vardır ki insanların çoğu bunlarda aldanmıştır: sağlık ve boş vakit.", "Buhârî 6412"},
        {"Dünyada bir garip ya da bir yolcu gibi ol.", "Buhârî"},
        {"Allah'a gereği gibi tevekkül etseydiniz, kuşları rızıklandırdığı gibi sizi de rızıklandırırdı: sabah aç çıkar, akşam tok dönerler.", "Tirmizî 2344"},
        {"Mümin aynı delikten iki defa ısırılmaz.", "Buhârî ve Müslim"},
        {"Cennet, nefsin hoşlanmadığı şeylerle; cehennem ise nefsin arzularıyla çevrilmiştir.", "Müslim"},
    };
}
