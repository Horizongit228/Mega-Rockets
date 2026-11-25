package dev.megarockets;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import java.util.stream.Stream;

public final class MoonBiomeSource extends BiomeSource {
 public static final MapCodec<MoonBiomeSource> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
  Biome.REGISTRY_CODEC.fieldOf("plains").forGetter(s->s.plains),
  Biome.REGISTRY_CODEC.fieldOf("highlands").forGetter(s->s.highlands),
  Biome.REGISTRY_CODEC.fieldOf("craters").forGetter(s->s.craters),
  Biome.REGISTRY_CODEC.fieldOf("forest").forGetter(s->s.forest)
 ).apply(i,MoonBiomeSource::new));
 private final RegistryEntry<Biome> plains,highlands,craters,forest;
 private volatile MoonTerrain terrain=new MoonTerrain(0);
 public MoonBiomeSource(RegistryEntry<Biome> plains,RegistryEntry<Biome> highlands,RegistryEntry<Biome> craters,RegistryEntry<Biome> forest){
  this.plains=plains;this.highlands=highlands;this.craters=craters;this.forest=forest;
 }
 public void setTerrain(MoonTerrain terrain){this.terrain=terrain;}
 @Override protected MapCodec<? extends BiomeSource> getCodec(){return CODEC;}
 @Override protected Stream<RegistryEntry<Biome>> biomeStream(){return Stream.of(plains,highlands,craters,forest);}
 @Override public RegistryEntry<Biome> getBiome(int x,int y,int z,MultiNoiseUtil.MultiNoiseSampler noise){
  return switch(terrain.region(x*4,z*4)){case PLAINS->plains;case HIGHLANDS->highlands;case CRATERS->craters;case FOREST->forest;};
 }
}
