package train.client.render.renderSwitch;

import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import fexcraft.tmt.slim.Tessellator;
import train.client.render.renderSwitch.models.ModelcircleSwitchStandOff;
import train.client.render.renderSwitch.models.ModelcircleSwitchStandOn;
import train.common.library.Info;
import train.common.tile.switchStand.TileCircleSwitchStand;

public class RendercircleSwitchStand extends TileEntitySpecialRenderer {
    private static final ModelcircleSwitchStandOn modelSwitch2 = new ModelcircleSwitchStandOn();
    private static final ModelcircleSwitchStandOff modelSwitch = new ModelcircleSwitchStandOff();
    private static final ResourceLocation texture2 = new ResourceLocation(Info.resourceLocation,Info.modelTexPrefix + "circleswitchon.png");
    private static final ResourceLocation texture = new ResourceLocation(Info.resourceLocation,Info.modelTexPrefix + "circleswitchoff.png");

    @Override
    public void renderTileEntityAt(TileEntity tileEntity, double x, double y, double z, float tick) {
        boolean powered = tileEntity.getWorldObj().getBlock(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord).isProvidingWeakPower(tileEntity.getWorldObj(), tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, 0) > 0;
        Tessellator.bindTexture(powered?texture2:texture);

        GL11.glPushMatrix();
        GL11.glTranslated(x+0.5,y+0.6,z+0.5);
        GL11.glRotated(180,0,1,0);
        boolean skipRender = false;

        switch (((TileCircleSwitchStand)tileEntity).getFacing()){
            case NORTH:{
                GL11.glRotated(180,0,0,1);
                GL11.glRotated(270,0,1,0);
                GL11.glTranslated(0.1875,0,0.125);
                break;
            }
            case SOUTH:{
                GL11.glRotated(180,0,0,1);
                GL11.glRotated(90,0,1,0);
                GL11.glTranslated(0.1875,0,0.125);
                break;
            }
            case EAST:{
                GL11.glRotated(180,0,0,1);
                GL11.glRotated(0,0,1,0);
                GL11.glTranslated(0.1875,0,0.125);
                break;
            }
            case WEST:{
                GL11.glRotated(180,0,0,1);
                GL11.glRotated(180,0,1,0);
                GL11.glTranslated(0.1875,0,0.125);
                break;
            }
            default:{
                skipRender = true;
            }
        }

        if (!skipRender) {
            if (powered) {
                modelSwitch2.render(null, 0, 0, 0, 0, 0, 0.0625f);
            } else {
                modelSwitch.render(null, 0, 0, 0, 0, 0, 0.0625f);
            }
        }
        GL11.glPopMatrix();
    }
}