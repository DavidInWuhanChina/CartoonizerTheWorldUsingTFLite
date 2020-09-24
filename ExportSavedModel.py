import sys
sys.path.append('./White-box-Cartoonization/test_code')

import matplotlib.pyplot as plt
import cartoonize
import network
import guided_filter

import tensorflow.compat.v1 as tf

tf.disable_eager_execution()

model_path = './White-box-Cartoonization/test_code/saved_models'

tf.reset_default_graph()

config  = tf.ConfigProto()
config.gpu_options.allow_growth = True

with tf.Session(config=config) as sess:
    #Create placeholder for the input
    input_photo = tf.placeholder(tf.float32, [1, None, None, 3], name='input_photo')

    #Run the input placeholder through the generator, and then apply a filter to process the generator output
    network_out = network.unet_generator(input_photo)
    final_out = guided_filter.guided_filter(input_photo, network_out, r=1, eps=5e-3)
    final_out = tf.identity(final_out, name='final_output') #Create an identity filtering layer创建一个身份过滤层

    #The above process is basically needed to construct the computation graph for the current session
    # 基本上需要上述过程来构造当前会话的计算图

    #Get the genrator variables and restore the pre-trained chechpoints in the current session
    all_vars = tf.trainable_variables()
    gene_vars = [var for var in all_vars if 'generator' in var.name]
    saver = tf.train.Saver(var_list=gene_vars)
    sess.run(tf.global_variables_initializer())
    saver.restore(sess, tf.train.latest_checkpoint(model_path))

    #Export to SavedModel
    tf.saved_model.simple_save(
        sess,
        './saved_model_dir',
        inputs={input_photo.name: input_photo},
        outputs={final_out.name: final_out}
    )

