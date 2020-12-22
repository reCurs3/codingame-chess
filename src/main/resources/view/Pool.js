export class Pool {
    constructor (factory) {
        this.factory = factory;
        this.objects = [];
    }

    acquire() {
        var obj;
        if (this.objects.length != 0)
            obj = this.objects.pop();
        else
            obj = this.factory();
        return obj;
    }

    release(obj) {
        if (!obj) return;
        this.objects.push(obj);
    }

    clear() {
        this.objects.length = 0;
    }
}