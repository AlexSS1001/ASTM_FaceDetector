package com.example.alex.astm_facedetector;

import org.opencv.core.CvType;
import org.opencv.core.Rect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import java.lang.Math;

public class FaceDetector
{
    private Mat frame;
    private CascadeClassifier mJavaDetector;
    private float rotation;

    public FaceDetector(CascadeClassifier mJavaDetector)
    {
        this.mJavaDetector = mJavaDetector;
    }

    public void setFrame(Mat frame)
    {
        this.frame = frame;
    }

    public void setRotation(float rotation)
    {
        this.rotation = rotation;
    }

    private Rect upScaleRectangle(Rect bb, int ratio, int orientation)
    {
        Point tl = bb.tl();
        Point br = bb.br();
        Point bb_center = new Point(bb.tl().x + bb.width/2, bb.tl().y + bb.height/2);
        Point scaled_bb_center = new Point(ratio * bb_center.x, ratio * bb_center.y);

        tl.x = scaled_bb_center.x - bb.width/2 * ratio;
        tl.y = scaled_bb_center.y - bb.height/2 * ratio;
        br.x = scaled_bb_center.x + bb.width/2 * ratio;
        br.y = scaled_bb_center.y + bb.height/2 * ratio;
        double w = br.x - tl.x;
        double h = br.y - tl.y;


        Point tl_temp;
        Point br_temp;
        switch (orientation)
        {
            case -90:
                //landscape flipped
                br.y = frame.height() - tl.y;
                tl.y = frame.height() - (tl.y + h);
                break;
            case 0:
                //portrait default
                tl.x = scaled_bb_center.y - h/2;
                tl.y = scaled_bb_center.x - w/2;
                br.x = scaled_bb_center.y + h/2;
                br.y = scaled_bb_center.x + w/2;

                break;
            case 180:
                //portrait flipped
                scaled_bb_center.y = frame.width() - scaled_bb_center.y;

                tl.x = scaled_bb_center.y - h/2;
                tl.y = scaled_bb_center.x - w/2;
                br.x = scaled_bb_center.y + h/2;
                br.y = scaled_bb_center.x + w/2;

                break;
            default:
                //landscape normal
                break;

        }
        Rect r_scaled = new Rect(tl, br);
        return r_scaled;
    }

    public Mat detectFaces()
    {
        if (!frame.empty() && !mJavaDetector.empty())
        {

            final int ratio = 4;

            Mat grayFrame = new Mat(frame.width(), frame.height(), CvType.CV_8UC1);
            Mat scaledImg = new Mat(frame.width()/ratio, frame.height()/ratio, CvType.CV_8UC1);
            Mat eqImg = new Mat(frame.width()/ratio, frame.height()/ratio, CvType.CV_8UC1);
            Size scale = new Size(grayFrame.height()/ratio, grayFrame.width()/ratio);

            Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
            Imgproc.resize(grayFrame, scaledImg, scale);
            Imgproc.equalizeHist(scaledImg, eqImg);

            //detect faces
            float mRelativeFaceSize = 0.2f;
            int mAbsoluteFaceSize   = 0;
            if (mAbsoluteFaceSize == 0)
            {
                int height = eqImg.rows();
                if (Math.round(height * mRelativeFaceSize) > 0)
                {
                    mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
                }
            }

            MatOfRect faces = new MatOfRect();
            //landscape default
            if (rotation >= 45 && rotation <= 135)
            {
                mJavaDetector.detectMultiScale(eqImg, faces, 1.05, 4, 6,
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size(grayFrame.width(),grayFrame.height()));
                Rect[] facesArray_90 = faces.toArray();

                if (facesArray_90.length > 0)
                {
                    Scalar color = new Scalar(0,255,0);
                    for (int i = 0; i < facesArray_90.length; i++)
                    {
                        facesArray_90[i] = upScaleRectangle(facesArray_90[i], ratio, 90);
                        Core.rectangle(frame, facesArray_90[i].tl(), facesArray_90[i].br(), color, 3);
                    }
                }
            }
            //landscape flipped
            if (rotation <= -45 && rotation >= -135)
            {
                Mat eqImgFlipped = eqImg;
                Core.flip(eqImg, eqImgFlipped, 0);
                mJavaDetector.detectMultiScale(eqImgFlipped, faces, 1.05, 4, 6,
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size(grayFrame.width(),grayFrame.height()));
                Rect[] facesArray_m90 = faces.toArray();

                if (facesArray_m90.length > 0)
                {
                    Scalar color = new Scalar(0,255,0);
                    for (int i = 0; i < facesArray_m90.length; i++)
                    {
                        facesArray_m90[i] = upScaleRectangle(facesArray_m90[i], ratio, -90);
                        Core.rectangle(frame, facesArray_m90[i].tl(), facesArray_m90[i].br(), color, 3);
                    }
                }
            }
            //portrait default
            if (rotation > -45 && rotation < 45)
            {
                Mat eqImgTransposed = eqImg.t();
                mJavaDetector.detectMultiScale(eqImgTransposed, faces, 1.05, 4, 6,
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size(grayFrame.width(),grayFrame.height()));
                Rect[] facesArray_0 = faces.toArray();

                if (facesArray_0.length > 0)
                {
                    Scalar color = new Scalar(0,255,0);
                    for (int i = 0; i < facesArray_0.length; i++)
                    {
                        facesArray_0[i] = upScaleRectangle(facesArray_0[i], ratio, 0);
                        Core.rectangle(frame, facesArray_0[i].tl(), facesArray_0[i].br(), color, 3);
                    }
                }
            }
            //portrait flipped
            if ((rotation > 135 && rotation <= 180) || (rotation < -135 && rotation >= -180))
            {
                Mat eqImgTransposed = eqImg.t();
                Mat eqImgFlipped = eqImgTransposed;
                Core.flip(eqImgTransposed, eqImgFlipped, 0);
                mJavaDetector.detectMultiScale(eqImgFlipped, faces, 1.05, 4, 6,
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size(grayFrame.width(),grayFrame.height()));
                Rect[] facesArray_180 = faces.toArray();

                if (facesArray_180.length > 0)
                {
                    Scalar color = new Scalar(0,255,0);
                    for (int i = 0; i < facesArray_180.length; i++)
                    {
                        facesArray_180[i] = upScaleRectangle(facesArray_180[i], ratio, 180);
                        Core.rectangle(frame, facesArray_180[i].tl(), facesArray_180[i].br(), color, 3);
                    }
                }
            }
        }
        return frame;
    }
}
