# CartoonizerTheWorldUsingTFLite
The TFlite model used [a Generative Adversarial Network (GAN) model proposed in this CVPR 2020 paper Learning to Cartoonize Using White-box Cartoon Representations](https://github.com/SystemErrorWang/White-box-Cartoonization/blob/master/paper/06791.pdf). [The pretrained weights were provided by the authors of the paper and available in their project GitHub repository here](https://github.com/SystemErrorWang/White-box-Cartoonization).


# Citation

The original authors of White-box CartoonGAN are Xinrui Wang and Jinze Yu.

`@InProceedings{Wang_2020_CVPR, 
author = {Wang, Xinrui and Yu, Jinze,     
title = {Learning to Cartoonize Using White-Box Cartoon Representations,   
booktitle = {IEEE/CVF Conference on Computer Vision and Pattern Recognition (CVPR)},   
month = {June}, year = {2020} }`

fork [margaretmz's Cartoonizer-with-TFLite](https://github.com/margaretmz/Cartoonizer-with-TFLite)

python reqirement:

`pip install -q tf-nightly`


`python ExportSavedModel.py`

`python ConvertTfLite.py`

`python GenerateImg.py`

`python ESRGAN.py`

Android requirement

`gradle-6.5-all.zip`

`Android studio 4.1RC3`






