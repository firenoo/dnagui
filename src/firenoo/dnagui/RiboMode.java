package firenoo.dnagui;


import javafx.beans.binding.StringBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Represents a preset ribosome interpretation for easier editing.
 * File created on 9/5/2020
 */
public class RiboMode {

    private String name;
    private List<DnaEntry> traits;

    public static final int PARTIAL_5050 = (12 << 8) | (8 << 24);
    public static final int PARTIAL_6733 = (8 << 8);
    public static final int PARTIAL_7525 = (12 << 8);

    public RiboMode(String name) {
        this.name = name;
        this.traits = new ArrayList<>();
    }

    public RiboMode(String name, int traitCt) {
        this.name = name;
        this.traits = new ArrayList<>(traitCt);
        for(int i = 0; i < traitCt; i++) {
            traits.add(null);
        }
    }


    public RiboMode addTrait(String name, int loc, int defaultDom) {
        DnaEntry entry = new DnaEntry(name, loc, defaultDom);
        for(int i = traits.size(); i < loc; i++) {
            traits.add(null);
        }
        traits.set(loc, entry);

        return this;
    }

    public List<DnaEntry> getTraits() {
        return traits;
    }

    public String getName() {
        return name;
    }

    /**
     * A DNA (Trait) Entry. Encapsulates trait data and name.
     */
    public static class DnaEntry {

        private String defaultName;
        private int defaultLoc, defaultData;

        private SimpleStringProperty name;
        private AlleleProperty data;
        private SimpleIntegerProperty loc;

        private DnaEntry(String name, int loc, int data) {
            this.defaultName = name;
            this.defaultLoc = loc;
            this.defaultData = data;
            this.name = new SimpleStringProperty(name);
            this.loc = new SimpleIntegerProperty(loc);
            this.data = new AlleleProperty(data);
        }

        public ReadOnlyStringProperty getName() {
            return name;
        }

        public ReadOnlyIntegerProperty getLoc() {
            return loc;
        }

        public AlleleProperty getData() {
            return data;
        }

        public void resetToDefault() {
            this.name.set(defaultName);
            this.loc.set(defaultLoc);
            this.data.set(defaultData);
        }

        public void setData(int newData) {
            this.data.set(newData);
        }

        public void setName(String newName) {
            this.name.set(newName);
        }
    }

    public static final RiboMode DEFAULT_FIRENOO;
//    public static final RiboMode CUSTOM;

    private static final int GENB_L = 0; //General bonus
    private static final int GRTF_L = 1; //Growth factor
    private static final int GRTS_L = 2; //Growth speed
    private static final int GRTE_L = 3; //Growth efficiency
    private static final int GRTB_L = 4; //Growth bonus
    private static final int FDST_L = 5; //Food storage
    private static final int FDDI_L = 6; //Food digestion
    private static final int FDAB_L = 7; //food absorption
    private static final int ENDU_L = 8; //Endurance
    private static final int VSRG_L = 9; //Vision range
    private static final int WAND_L = 10; //Wanderer
    private static final int CMPT_L = 11; //Competitive
    private static final int RATN_L = 12; //Rationing
    private static final int PEF1_L = 13; //Prod/Eff 1
    private static final int PEF2_L = 14; //Prod/Eff 2
    private static final int MEMS_L = 15; //Memory size and forgetOrder
    private static final int MEMF_L = 16; //Memory forget policy

    static {
        DEFAULT_FIRENOO = new RiboMode("firenoo-default", 17);
        DEFAULT_FIRENOO.addTrait("general_bonus", GENB_L, PARTIAL_5050)
                       .addTrait("growth_factor", GRTF_L, PARTIAL_5050)
                       .addTrait("growth_speed", GRTS_L, PARTIAL_5050)
                       .addTrait("growth_eff", GRTE_L, PARTIAL_6733)
                       .addTrait("growth_bonus", GRTB_L, PARTIAL_6733)
                       .addTrait("food_storage", FDST_L, PARTIAL_5050)
                       .addTrait("food_digest", FDDI_L, PARTIAL_5050)
                       .addTrait("food_abs", FDAB_L, PARTIAL_5050)
                       .addTrait("endurance", ENDU_L, PARTIAL_7525)
                       .addTrait("vision", VSRG_L, PARTIAL_5050)
                       .addTrait("wanderer", WAND_L, PARTIAL_5050)
                       .addTrait("competitive", CMPT_L, PARTIAL_6733)
                       .addTrait("rationing", RATN_L, PARTIAL_5050)
                       .addTrait("prodeff1", PEF1_L, PARTIAL_5050)
                       .addTrait("prodeff2", PEF2_L, PARTIAL_5050)
                       .addTrait("mem_size", MEMS_L, PARTIAL_5050)
                       .addTrait("forgetful", MEMF_L, PARTIAL_5050);
//        CUSTOM = new RiboMode("unnamed");
    }

    public static final class AlleleProperty extends SimpleIntegerProperty {

        public AlleleProperty(int value) {
            super(value);
        }

        public StringBinding of(Function<Integer, String> func) {
            return new StringBinding() {
                {
                    super.bind(AlleleProperty.this);
                }
                @Override
                protected String computeValue() {
                    return func.apply(AlleleProperty.this.get());
                }
            };

        }

        public StringBinding of(int mask, int offset, IntegerProperty mode) {
            return new StringBinding() {
                {
                    super.bind(mode, AlleleProperty.this);
                }
                @Override
                protected String computeValue() {
                    int val = (AlleleProperty.this.get() >>> offset) & mask;
                    switch (mode.get()) {
                        case 1:
                            return DnaGui.pHexStr(val, 2);
                        case 2:
                            return String.valueOf(val);
                        default:
                            return DnaGui.pBinaryStr(val, 8);
                    }
                }
            };
        }

    }

}
