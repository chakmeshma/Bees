package net.chakmeshma.brutengine.rendering;


import android.graphics.Bitmap;
import android.opengl.GLUtils;

import net.chakmeshma.brutengine.development.exceptions.InitializationException;
import net.chakmeshma.brutengine.utilities.AssetsUtilities;

import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexParameteri;

/**
 * Created by chakmeshma on 13.09.2017.
 */

public class Texture {
    private int textureID;
    private String fileName;

    public Texture(String textureFileName) throws InitializationException {
        int[] textureIDS = new int[1];

        glGenTextures(1, textureIDS, 0);

        glBindTexture(GL_TEXTURE_2D, textureIDS[0]);

        Bitmap bitmap = AssetsUtilities.getBitmapFromAsset(textureFileName, false);

        if (bitmap == null)
            throw new InitializationException("texture file missing!");

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

        bitmap.recycle();

        this.textureID = textureIDS[0];
        this.fileName = textureFileName;

    }

//    public void activateCorrespondingTextureUnit() {
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureUnitIndex);
//    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, getTextureID());
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getTextureID() {
        return textureID;
    }
}
