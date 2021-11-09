const socketio = require('socket.io');
const request = require('request');
const compile = require('./compile');

module.exports = (server) => {
    const io = socketio(server, { path: '/socket.io' });

    // connection event
    io.on('connection', (socket) => {
        console.log('new client connection! socket.id : ', socket.id);

        // disconnect event
        socket.on('disconnect', () => {
            console.log('client disconnect! socket.id : ', socket.id);
        });

        // client join room
        socket.on('join', (data)=>{
            const {roomId} = data;

            // if data has roomId
            if(roomId){
                socket.join(roomId);
                socket.roomId = roomId;

                console.log(socket.id,' join ',roomId);
                io.to(roomId).emit('join',{
                    'chat' : socket.id + ' is join!'
                });
            }
            // if data has not roomId
            else{
                console.log('no roomId');
            }
        })

        // change language
        socket.on('language', (data)=>{
            const roomId = socket.roomId;
            const {language} = data;

            // if data has roomId and language
            if(roomId && language) io.to(roomId).emit('language', language);
        } )

        // compile code
        socket.on('compile', async (data) => {
            const roomId = socket.roomId;

            let compile_result = null;
            try{
                compile_result = await compile(data);
            }catch(err){
                compile_result = err;
            }

            if(roomId) io.to(roomId).emit('compile', compile_result);
        });
    });
}