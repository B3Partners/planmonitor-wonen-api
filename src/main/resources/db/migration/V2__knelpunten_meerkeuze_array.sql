alter table planregistratie alter column knelpunten_meerkeuze
        type pmw_knelpunten_meerkeuze[]
        using case when knelpunten_meerkeuze is null then array[]::pmw_knelpunten_meerkeuze[] else array[knelpunten_meerkeuze] end;